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
import io.fabric8.collector.BuildConfigProcessor;
import io.fabric8.collector.NamespaceName;
import io.fabric8.collector.elasticsearch.ResultsDTO;
import io.fabric8.collector.git.elasticsearch.CommitDTO;
import io.fabric8.collector.git.elasticsearch.ElasticsearchClient;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.JsonHelper;
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
import org.gitective.core.filter.commit.CommitLimitFilter;
import org.gitective.core.filter.commit.CommitListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 */
public class GitBuildConfigProcessor implements BuildConfigProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitBuildConfigProcessor.class);

    private final ElasticsearchClient elasticsearchClient;
    private final File cloneFolder;
    private int commitLimit;

    public GitBuildConfigProcessor(ElasticsearchClient elasticsearchClient, File cloneFolder, int commitLimit) {
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

    @Override
    public void process(NamespaceName name, BuildConfig buildConfig) throws Exception {
        GitBuildSource git = gitBuildSource(buildConfig);
        if (git != null) {
            String uri = git.getUri();
            if (Strings.isNotBlank(uri)) {
                processGitRepo(name, buildConfig, git, uri);
            }
        }
    }

    protected void processGitRepo(NamespaceName name, BuildConfig buildConfig, GitBuildSource git, String uri) throws IOException {
        File namespaceFolder = new File(cloneFolder, name.getNamespace());
        File nameFolder = new File(namespaceFolder, name.getName());
        nameFolder.mkdirs();
        UserDetails userDetails = new UserDetails();
        String branch = git.getRef();
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        File gitFolder = cloneOrPullRepository(userDetails, nameFolder, uri, branch);

        processHistory(name, gitFolder, buildConfig, uri, branch);
    }

    /**
     * Lets process the commit history going back in time until we have persisted all the commits into Elasticsearch
     */
    protected void processHistory(NamespaceName name, File gitFolder, BuildConfig buildConfig, String uri, String branch) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(gitFolder)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        Git git = new Git(repository);

        Repository r = git.getRepository();

        try {
            getHEAD(git);
        } catch (Exception e) {
            LOG.error("Cannot find HEAD of the git repository for " + name + ": " + e, e);
            return;
        }

        CommitFinder finder = new CommitFinder(r);
        CommitListFilter filter = new CommitListFilter();
        finder.setFilter(filter);

        // TODO lets load the latest firstObjectId written to Elasticsearch
        // along with the earliest!

        // load the last found object id?
        String firstObjectId = null;
        if (Strings.isNotBlank(firstObjectId)) {
            finder.findFrom(firstObjectId);
        } else {
            boolean findFromBranch = false;
            if (findFromBranch) {
                ObjectId branchObjectId = getBranchObjectId(git, branch);
                System.out.println("Finding from branchObjectId: " + branchObjectId + " for branch: " + branch);
                if (branchObjectId != null) {
                    finder = finder.findUntil(branchObjectId);
                } else {
                    finder = finder.findInBranches();
                }
            } else {
                finder.find();
            }
        }
        List<RevCommit> commits = filter.getCommits();
        int counter = 0;
        for (RevCommit entry : commits) {
            if (commitLimit > 0) {
                if (++counter > commitLimit) {
                    return;
                }
            }
            processCommit(name, git, entry, buildConfig, uri, branch);
        }
        if (commits.size() == 0) {
            // TODO
            // lets try find older commits from before our last entry!
        }
    }

    protected void processCommit(NamespaceName projectName, Git git, RevCommit commit, BuildConfig buildConfig, String uri, String branch) throws JsonProcessingException {
        CommitDTO dto = new CommitDTO(git, projectName, commit, uri, branch);
        String sha = dto.getSha();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing commit " + dto.getName() + " message: " + dto.getShortMessage());
        }
        String index = "git";
        String type = "commit";

        ResultsDTO results = elasticsearchClient.storeCommit(index, type, sha, dto);
        LOG.info("Results: " + JsonHelper.toJson(results));
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
