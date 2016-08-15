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

import io.fabric8.collector.NamespaceName;
import io.fabric8.collector.elasticsearch.EmbeddedElasticsearchTestSupport;
import io.fabric8.collector.git.elasticsearch.GitElasticsearchClient;
import io.fabric8.utils.Files;
import org.eclipse.jgit.api.Git;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static io.fabric8.collector.git.GitHelpers.gitFromGitFolder;
import static org.junit.Assert.assertEquals;

/**
 */
public class GitCollectorTest extends EmbeddedElasticsearchTestSupport {
    protected int commitLimit = 2;
    protected String gitUrl = "https://github.com/jstrachan/test-git-collector.git";
    protected String branch = "master";
    protected NamespaceName namespaceName = new NamespaceName("default", "myapp");
    protected long pollPeriod = 4 * 1000;

    @Test
    public void testCollectionPopulatesElasticSearch() throws Exception {
        GitElasticsearchClient elasticsearchClient = new GitElasticsearchClient("http://127.0.0.1/", elasticsearchPort, null, null);
        File cloneFolder = new File(getBaseDir(), "target/test-git-clone/sample");
        Files.recursiveDelete(cloneFolder);
        cloneFolder.getParentFile().mkdirs();

        GitBuildConfigProcessor processor = new GitBuildConfigProcessor(elasticsearchClient, cloneFolder, commitLimit);

        assertProcessGitrepoCommits(processor, 2, true);
        assertProcessGitrepoCommits(processor, 2, true);
        assertProcessGitrepoCommits(processor, 1, true);
        assertProcessGitrepoCommits(processor, 0, true);


        // now lets make a commit and check we get a new item pushed to ES
        File projectDir = new File(cloneFolder, namespaceName.getNamespace() + "/" + namespaceName.getName());

        Files.writeToFile(new File(projectDir, "README.md"), "Dummy-commit!".getBytes());
        Git git = gitFromGitFolder(new File(projectDir, ".git"));
        git.commit().setMessage("Dummy test commit").call();

        System.out.println("Just done a commit!");

        assertProcessGitrepoCommits(processor, 1, true);
        assertProcessGitrepoCommits(processor, 0, true);
    }



    protected void assertProcessGitrepoCommits(GitBuildConfigProcessor processor, int expectedCount, boolean sleep) throws IOException, InterruptedException {
        int actual = processor.processGitRepo(namespaceName, gitUrl, branch);
        System.out.println("Processed " + actual + " commit(s)");
        assertEquals("number of commits", expectedCount, actual);
        if (sleep) {
            Thread.sleep(pollPeriod);
        }
    }


}
