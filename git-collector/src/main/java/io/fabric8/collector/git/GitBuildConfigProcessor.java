/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.collector.git;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.collector.BuildConfigProcessor;
import io.fabric8.collector.NamespaceName;
import io.fabric8.collector.elasticsearch.JsonNodes;
import io.fabric8.collector.elasticsearch.ResultsDTO;
import io.fabric8.collector.elasticsearch.SearchDTO;
import io.fabric8.collector.git.elasticsearch.CommitDTO;
import io.fabric8.collector.git.elasticsearch.GitElasticsearchClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.utils.Files;
import io.fabric8.utils.Function;
import io.fabric8.utils.Strings;
import io.fabric8.utils.jaxrs.JAXRSClients;
import io.fabric8.utils.jaxrs.JsonHelper;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.gitective.core.CommitFinder;
import org.gitective.core.CommitUtils;
import org.gitective.core.filter.commit.CommitListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import static io.fabric8.collector.git.elasticsearch.Searches.createMinMaxGitCommitSearch;
import static io.fabric8.kubernetes.api.Annotations.Builds.GIT_CLONE_URL;

/**
 */
public class GitBuildConfigProcessor implements BuildConfigProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitBuildConfigProcessor.class);

    private final GitElasticsearchClient elasticsearchClient;
    private final File cloneFolder;
    private int commitLimit;
    private String esIndex = "git";
    private String esType = "commit";
    private boolean initialised;


    public GitBuildConfigProcessor(GitElasticsearchClient elasticsearchClient, File cloneFolder, int commitLimit) {
        this.elasticsearchClient = elasticsearchClient;
        this.cloneFolder = cloneFolder;
        this.commitLimit = commitLimit;
    }

    public static void cloneRepo(File projectFolder, String cloneUrl, CredentialsProvider credentialsProvider, final File sshPrivateKey, final File sshPublicKey, String remote) {
        // clone the repo!
        boolean cloneAll = false;
        LOG.info("Cloning git repo " + cloneUrl + " into directory " + projectFolder.getAbsolutePath());
        CloneCommand command = Git.cloneRepository();
        GitHelpers.configureCommand(command, credentialsProvider, sshPrivateKey, sshPublicKey);
        command = command.setCredentialsProvider(credentialsProvider).
                setCloneAllBranches(cloneAll).setURI(cloneUrl).setDirectory(projectFolder).setRemote(remote);

        try {
            command.call();
        } catch (Throwable e) {
            LOG.error("Failed to command remote repo " + cloneUrl + " due: " + e.getMessage(), e);
            throw new RuntimeException("Failed to command remote repo " + cloneUrl + " due: " + e.getMessage());
        }
    }

    public static GitBuildSource gitBuildSource(BuildConfig buildConfig) {
        GitBuildSource git = null;
        BuildConfigSpec spec = buildConfig.getSpec();
        if (spec != null) {
            BuildSource source = spec.getSource();
            if (source != null) {
                git = source.getGit();
            }
        }
        return git;
    }

    /**
     * A helper method to handle REST APIs which throw a 404 by just returning null
     */
    public static <T> T handle404ByReturningNull(Callable<T> callable) {
        try {
            return callable.call();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public void process(NamespaceName name, BuildConfig buildConfig) throws Exception {
        String uri = KubernetesHelper.getOrCreateAnnotations(buildConfig).get(GIT_CLONE_URL);
        GitBuildSource git = gitBuildSource(buildConfig);
        if (git != null) {
            if (Strings.isNullOrBlank(uri)) {
                uri = git.getUri();
            }
            if (Strings.isNotBlank(uri)) {
                processGitRepo(name, buildConfig, git, uri);
            }
        }
    }

    protected void checkInitialised() throws JsonProcessingException {
        if (!initialised) {
            configureMappings();
            initialised = true;
        }
    }

    protected void configureMappings() throws JsonProcessingException {
        ObjectNode results = elasticsearchClient.createIndexIfMissing(esIndex, esType, new Function<ObjectNode, Boolean>() {
            @Override
            public Boolean apply(ObjectNode index) {
                return true;
            }
        });
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated index results: " + JsonHelper.toJson(results));
        }

        // now lets update mappings
        results = elasticsearchClient.createIndexMappingIfMissing(esIndex, esType, new Function<ObjectNode, Boolean>() {
            @Override
            public Boolean apply(ObjectNode properties) {
                String[] notAnalysed = {"app", "namespace", "branch", "name", "sha", "repo_url"};
                for (String propertyName : notAnalysed) {
                    ObjectNode property = JsonNodes.setObjects(properties, propertyName);
                    JsonNodes.set(property, "index", "not_analyzed");
                    if (!property.has("type")) {
                        JsonNodes.set(property, "type", "string");
                    }
                }
                String[] timeProperties = {"commit_time"};
                for (String propertyName : timeProperties) {
                    ObjectNode property = JsonNodes.setObjects(properties, propertyName);
                    JsonNodes.set(property, "type", "date");
                    JsonNodes.set(property, "format", "strict_date_optional_time||epoch_millis");
                }
                return true;
            }
        });
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated mapping results: " + JsonHelper.toJson(results));
        }
    }

    /**
     * This method is public for easier unit testing
     */
    public int processGitRepo(NamespaceName name, String gitUrl, String gitRef) throws IOException {
        BuildConfig buildConfig = new BuildConfig();
        BuildConfigSpec buildConfigSpec = new BuildConfigSpec();
        buildConfig.setSpec(buildConfigSpec);
        BuildSource buildSource = new BuildSource();
        buildSource.setType("Git");
        GitBuildSource gitSource = new GitBuildSource();
        gitSource.setUri(gitUrl);
        if (Strings.isNullOrBlank(gitRef)) {
            gitRef = "master";
        }
        gitSource.setRef(gitRef);
        buildSource.setGit(gitSource);
        buildConfigSpec.setSource(buildSource);
        return processGitRepo(name, buildConfig, gitSource, gitUrl);
    }

    protected int processGitRepo(NamespaceName name, BuildConfig buildConfig, GitBuildSource git, String uri) throws IOException {
        // we may need to modify the schema now!
        checkInitialised();

        File namespaceFolder = new File(cloneFolder, name.getNamespace());
        File nameFolder = new File(namespaceFolder, name.getName());
        nameFolder.mkdirs();
        UserDetails userDetails = new UserDetails();
        String branch = git.getRef();
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        File gitFolder = cloneOrPullRepository(userDetails, nameFolder, uri, branch);

        return processHistory(name, gitFolder, buildConfig, uri, branch);
    }

    /**
     * Lets process the commit history going back in time until we have persisted all the commits into Elasticsearch
     */
    protected int processHistory(NamespaceName name, File gitFolder, BuildConfig buildConfig, String uri, String branch) throws IOException {
        Git git = GitHelpers.gitFromGitFolder(gitFolder);

        Repository r = git.getRepository();

        try {
            getHEAD(git);
        } catch (Exception e) {
            LOG.error("Cannot find HEAD of the git repository for " + name + ": " + e, e);
            return 0;
        }

        CommitFinder finder = new CommitFinder(r);
        CommitListFilter filter = new CommitListFilter();
        finder.setFilter(filter);

        finder.find();
        List<RevCommit> commits = filter.getCommits();
        commits = filterAndSortCommits(name, branch, commits);

        int counter = 0;
        for (RevCommit commit : commits) {
            processCommit(name, git, commit, buildConfig, uri, branch);
            if (commitLimit > 0) {
                if (++counter >= commitLimit) {
                    break;
                }
            }
        }

        if (counter > 0) {
            LOG.info(name + " Processed " + counter + " commit(s)");
        }
        return counter;
    }

    /**
     * Lets filter and sort the commits to filter out any commits we have already processed
     * using the newest and oldest commit sha in Elasticsearch.
     * Any newer commits we process in reverse order, oldest first - so that we keep a continuous
     * range of commits in Elasticsearch at all times - to avoid repeatedly posting data.
     * <p/>
     * When we catch up, there should be no need to post any more data; just a query now and again to see
     * if any newer or older commits are available.
     */
    protected List<RevCommit> filterAndSortCommits(NamespaceName name, String branch, List<RevCommit> commits) {
        String namespace = name.getNamespace();
        String app = name.getName();

        if (commits.size() == 0) {
            return commits;
        }
        String newestSha = findFirstId(createMinMaxGitCommitSearch(namespace, app, branch, false));
        String oldsetSha = null;
        if (newestSha != null) {
            oldsetSha = findFirstId(createMinMaxGitCommitSearch(namespace, app, branch, true));
        }

        if (oldsetSha == null || newestSha == null) {
            return commits;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("" + name + " found newest SHA: " + newestSha + " oldest SHA: " + oldsetSha);
        }

        List<RevCommit> newCommits = new ArrayList<>();
        List<RevCommit> oldCommits = new ArrayList<>();

        boolean foundNewest = false;
        boolean foundOldset = false;
        for (RevCommit commit : commits) {
            String sha = commit.getName();
            if (Objects.equals(sha, newestSha)) {
                foundNewest = true;
            } else if (Objects.equals(sha, oldsetSha)) {
                foundOldset = true;
            } else {
                if (foundNewest) {
                    if (foundOldset) {
                        oldCommits.add(commit);
                    } else {
                        // lets ignore this old commit which is >= newest and <= oldest
                    }
                } else {
                    newCommits.add(commit);
                }
            }
        }

        // lets reverse the order of any new commits so we processes the oldest first
        // so we keep a continnuous block of commits between oldest <-> newest
        Collections.reverse(newCommits);
        newCommits.addAll(oldCommits);

        if (LOG.isDebugEnabled()) {
            LOG.debug("" + name + " found " + newCommits.size() + " commit(s)");
        }
        return newCommits;
    }

    protected String findFirstId(final SearchDTO search) {
        return JAXRSClients.handle404ByReturningNull(new Callable<String>() {
            @Override
            public String call() throws Exception {
                ObjectNode results = elasticsearchClient.search(esIndex, esType, search);
                JsonNode hitsArray = results.path("hits").path("hits");
                JsonNode idNode = hitsArray.path(0).path("_id");
                String latestSha = null;
                if (idNode.isTextual()) {
                    latestSha = idNode.textValue();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Searching for " + JsonHelper.toJson(search) + " => " + latestSha);
                    LOG.debug("Found hits " + hitsArray.size());
/*
                    LOG.debug("JSON: " + JsonHelper.toJson(results));
*/

                }
                return latestSha;
            }
        });
    }

    protected void processCommit(NamespaceName projectName, Git git, RevCommit commit, BuildConfig buildConfig, String uri, String branch) throws JsonProcessingException {
        CommitDTO dto = new CommitDTO(git, projectName, commit, uri, branch);
        String sha = dto.getSha();

        if (LOG.isDebugEnabled()) {
            LOG.debug(projectName + " processing commit: " + sha + " time: " + dto.getCommitTime() + " message: " + dto.getShortMessage());
        }

        ResultsDTO results = elasticsearchClient.storeCommit(esIndex, esType, sha, dto);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Results: " + JsonHelper.toJson(results));
        }
    }

    protected ObjectId getBranchObjectId(Git git, String branch) {
        Ref branchRef = null;
        try {
            String branchRevName = "refs/heads/" + branch;
            List<Ref> branches = git.branchList().call();
            for (Ref ref : branches) {
                String revName = ref.getName();
                if (Objects.equals(branchRevName, revName)) {
                    branchRef = ref;
                    break;
                }
            }
        } catch (GitAPIException e) {
            LOG.warn("Failed to find branches " + e, e);
        }

        ObjectId branchObjectId = null;
        if (branchRef != null) {
            branchObjectId = branchRef.getObjectId();
        }
        return branchObjectId;
    }

    protected String getHEAD(Git git) {
        RevCommit commit = CommitUtils.getHead(git.getRepository());
        return commit.getName();
    }

    protected File cloneOrPullRepository(UserDetails userDetails, File projectFolder, String cloneUrl, String branch) {
        File gitFolder = new File(projectFolder, ".git");
        CredentialsProvider credentialsProvider = userDetails.createCredentialsProvider();
        if (!Files.isDirectory(gitFolder) || !Files.isDirectory(projectFolder)) {
            // lets clone the git repository!
            cloneRepo(projectFolder, cloneUrl, credentialsProvider, userDetails.getSshPrivateKey(), userDetails.getSshPublicKey(), userDetails.getRemote());
        } else {
            doPull(gitFolder, credentialsProvider, branch, userDetails.createPersonIdent(), userDetails);
        }
        return gitFolder;
    }


    protected void doPull(File gitFolder, CredentialsProvider cp, String branch, PersonIdent personIdent, UserDetails userDetails) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitFolder)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            Git git = new Git(repository);

            File projectFolder = repository.getDirectory();

            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", userDetails.getRemote(), "url");
            if (Strings.isNullOrBlank(url)) {
                LOG.warn("No remote repository url for " + branch + " defined for the git repository at " + projectFolder.getCanonicalPath() + " so cannot pull");
                //return;
            }
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                LOG.warn("No merge spec for branch." + branch + ".merge in the git repository at " + projectFolder.getCanonicalPath() + " so not doing a pull");
                //return;
            }

            LOG.debug("Performing a pull in git repository " + projectFolder.getCanonicalPath() + " on remote URL: " + url);
            PullCommand pull = git.pull();
            GitHelpers.configureCommand(pull, userDetails);
            pull.setRebase(true).call();
        } catch (Throwable e) {
            LOG.error("Failed to pull from the remote git repo with credentials " + cp + " due: " + e.getMessage() + ". This exception is ignored.", e);
        }
    }
}
