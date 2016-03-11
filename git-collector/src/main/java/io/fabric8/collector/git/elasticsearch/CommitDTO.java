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

import io.fabric8.collector.NamespaceName;
import io.fabric8.collector.elasticsearch.DTOSupport;
import io.fabric8.collector.git.GitHelpers;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Represents information about a commit log or history
 */
public class CommitDTO extends DTOSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(CommitDTO.class);

    private final String namespace;
    private final String app;
    private final String repoUrl;
    private final String branch;
    private final String sha;
    private final PersonIdentDTO committer;
    private final String name;
    private final String shortMessage;
    private final String fullMessage;
    private final PersonIdentDTO author;
    private final Date commitTime;
    private int linesAdded;
    private int linesRemoved;

    public CommitDTO(Git git, NamespaceName projectNamespaceName, RevCommit commit, String repoUrl, String branch) {
        this.namespace = projectNamespaceName.getNamespace();
        this.app = projectNamespaceName.getName();
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.sha = commit.getId().getName();

        this.author = PersonIdentDTO.newInstance(commit.getAuthorIdent());
        this.committer = PersonIdentDTO.newInstance(commit.getCommitterIdent());
        this.fullMessage = commit.getFullMessage();
        this.name = commit.getName();
        this.commitTime = GitHelpers.getCommitDate(commit);
        this.shortMessage = commit.getShortMessage();

        // TODO how to figure out the number of lines added/removed from DiffEntry + HunkHeader?
        // lets try find out the lines added / updated / deleted for this commit
        try {
            Repository r = git.getRepository();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DiffFormatter formatter = createDiffFormatter(r, buffer);

            RevCommit baseCommit = null;
            RevTree commitTree = commit.getTree();
            RevTree baseTree;
            if (baseCommit == null) {
                if (commit.getParentCount() > 0) {
                    final RevWalk rw = new RevWalk(r);
                    RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
                    rw.dispose();
                    baseTree = parent.getTree();
                } else {
                    // FIXME initial commit. no parent?!
                    baseTree = commitTree;
                }
            } else {
                baseTree = baseCommit.getTree();
            }

            List<DiffEntry> diffEntries = formatter.scan(baseTree, commitTree);
            for (DiffEntry diffEntry : diffEntries) {
                formatter.format(diffEntry);

/*
                FileHeader fileHeader = formatter.toFileHeader(diffEntry);
                List<? extends HunkHeader> hunks = fileHeader.getHunks();
                for (HunkHeader hunk : hunks) {
                    // linesAdded += hunk.getOldImage().getLinesAdded();
                    // linesRemoved += hunk.getOldImage().getLinesDeleted();
                }
*/
            }
            // TODO should we store the diff? thats maybe too big?
            formatter.flush();
            String diff = buffer.toString();
            if (diff != null) {
                String[] lines = diff.split("\n");
                for (String line : lines) {
                    if (line.length() == 0 || line.startsWith("diff ") || line.startsWith("index ") || line.startsWith("--- ") || line.startsWith("+++ ")) {
                        continue;
                    }
                    if (line.startsWith("+")) {
                        linesAdded++;
                    } else if (line.startsWith("-")) {
                        linesRemoved++;
                    }
                }
            }

        } catch (IOException e) {
            LOG.warn("Failed to extract lines changed for " + projectNamespaceName + " branch: " + branch + " commit: " + sha + ". " + e, e);
        }
    }


    protected static DiffFormatter createDiffFormatter(Repository r, OutputStream buffer) {
        DiffFormatter formatter = new DiffFormatter(buffer);
        formatter.setRepository(r);
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        return formatter;
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

    public String getApp() {
        return app;
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

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(int linesRemoved) {
        this.linesRemoved = linesRemoved;
    }
}
