/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.collector.git.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.collector.NamespaceName;
import io.fabric8.collector.elasticsearch.DTOSupport;
import io.fabric8.collector.git.GitHelpers;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;

/**
 * Represents information about a commit log or history
 */
public class CommitDTO extends DTOSupport {
    private final String namespace;
    private final String project;
    private final String repoUrl;
    private final String branch;
    private final String sha;
    private final PersonIdentDTO committer;
    private final String name;
    private final String shortMessage;
    private final String fullMessage;
    private final PersonIdentDTO author;
    private final Date commitTime;

    @JsonProperty("@timestamp")
    private final Date timestamp;

    public CommitDTO(NamespaceName projectNamespaceName, RevCommit commit, String repoUrl, String branch) {
        this.namespace = projectNamespaceName.getNamespace();
        this.project = projectNamespaceName.getName();
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.sha = commit.getId().getName();

        this.author = PersonIdentDTO.newInstance(commit.getAuthorIdent());
        this.committer = PersonIdentDTO.newInstance(commit.getCommitterIdent());
        this.fullMessage = commit.getFullMessage();
        this.name = commit.getName();
        this.commitTime = GitHelpers.getCommitDate(commit);
        this.timestamp = commitTime;
        this.shortMessage = commit.getShortMessage();
    }


    @Override
    public String toString() {
        return "CommitInfo(sha " + sha + " author " + author + " commitTime " + commitTime + " " + shortMessage + ")";
    }

    public PersonIdentDTO getAuthor() {
        return author;
    }

    public String getBranch() {
        return branch;
    }

    public PersonIdentDTO getCommitter() {
        return committer;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getProject() {
        return project;
    }

    public String getSha() {
        return sha;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
