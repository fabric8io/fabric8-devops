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

import io.fabric8.annotations.Eager;
import io.fabric8.collector.BuildConfigCollectors;
import io.fabric8.collector.BuildConfigWatcher;
import io.fabric8.collector.git.elasticsearch.GitElasticsearchClient;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;

/**
 * Implements a Git collector of commit metrics for the git repositories for BuildConfig objects
 */
@ApplicationScoped
@Eager
public class GitCollector {

    private final GitBuildConfigProcessor processor;
    private final BuildConfigCollectors collectors;
    private final BuildConfigWatcher watcher;

    /**
     * Note this constructor is only added to help CDI work with the {@link Eager} extension
     */
    public GitCollector() throws Exception {
        this("gitCollectorWorkDir", 60000, 100, null);
    }

    @Inject
    public GitCollector(@ConfigProperty(name = "GIT_COLLECTOR_WORK_DIRECTORY", defaultValue = "gitCollectorWorkDir") String cloneFolder,
                        @ConfigProperty(name = "GIT_COLLECTOR_SLEEP_PERIOD_MILLIS", defaultValue = "60000") long sleepPeriodMillis,
                        @ConfigProperty(name = "GIT_COLLECTOR_COMMIT_LIMIT_PER_POLL", defaultValue = "100") int commitLimitPerPoll,
                        GitElasticsearchClient elasticsearchClient) throws Exception {
        this.processor = new GitBuildConfigProcessor(elasticsearchClient, new File(cloneFolder), commitLimitPerPoll);
        this.collectors = new BuildConfigCollectors(processor, sleepPeriodMillis);
        this.watcher = new BuildConfigWatcher(collectors);
    }
}
