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
package io.fabric8.collector;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of all the {@link BuildConfig} objects to be processed by a {@link BuildConfigProcessor}
 */
public class BuildConfigCollectors {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildConfigCollectors.class);

    private final BuildConfigProcessor buildConfigProcessor;
    private final long sleepPeriodMillis;
    private Map<NamespaceName, BuildConfig> map = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            pollAllBuildConfigs();
        }
    };

    public BuildConfigCollectors(BuildConfigProcessor buildConfigProcessor, long sleepPeriodMillis) {
        this.buildConfigProcessor = buildConfigProcessor;
        this.sleepPeriodMillis = sleepPeriodMillis;
        Objects.notNull(buildConfigProcessor, "buildConfigProcessor");
    }

    public void start() {
        schedulePollOfAllBuildConfigs();
    }

    public void addBuildConfig(BuildConfig buildConfig) {
        NamespaceName namespaceName = NamespaceName.create(buildConfig);
        map.put(namespaceName, buildConfig);
        scheduleBuildConfigPoll(namespaceName, buildConfig);
    }

    public void updateBuildConfig(BuildConfig buildConfig) {
        NamespaceName namespaceName = NamespaceName.create(buildConfig);
        map.put(namespaceName, buildConfig);
        scheduleBuildConfigPoll(namespaceName, buildConfig);
    }

    public void removeBuildConfig(BuildConfig buildConfig) {
        map.remove(NamespaceName.create(buildConfig));
    }

    protected void pollAllBuildConfigs() {
        Set<Map.Entry<NamespaceName, BuildConfig>> entries = map.entrySet();
        for (Map.Entry<NamespaceName, BuildConfig> entry : entries) {
            NamespaceName name = entry.getKey();
            BuildConfig buildConfig = entry.getValue();
            pollBuildConfig(name, buildConfig);
        }
        schedulePollOfAllBuildConfigs();
    }

    protected void pollBuildConfig(NamespaceName name, BuildConfig buildConfig) {
        LOG.debug("" + name + " processing started");
        try {
            buildConfigProcessor.process(name, buildConfig);
        } catch (Exception e) {
            LOG.error("Failed to process BuildConfig " + name + " due to: " + e, e);
        }
    }

    protected void scheduleBuildConfigPoll(final NamespaceName namespaceName, final BuildConfig buildConfig) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                pollBuildConfig(namespaceName, buildConfig);
            }
        });
    }

    protected ScheduledFuture<?> schedulePollOfAllBuildConfigs() {
        return executor.schedule(task, sleepPeriodMillis, TimeUnit.MILLISECONDS);
    }
}
