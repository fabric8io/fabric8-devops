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

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Watches {@link BuildConfig} objects in the current namespace and collects their data
 */
public class BuildConfigWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildConfigWatcher.class);

    private final BuildConfigCollectors collector;
    private String namespace = KubernetesHelper.defaultNamespace();
    private KubernetesClient client = new DefaultKubernetesClient();
    private Watch watch;

    public BuildConfigWatcher(BuildConfigCollectors aCollector) {
        this.collector = aCollector;
        OpenShiftClient openShiftClient = new Controller(client).getOpenShiftClientOrJenkinshift();
        Objects.notNull(openShiftClient, "No OpenShiftClient could be created!");

        this.watch = openShiftClient.buildConfigs().watch(new Watcher<BuildConfig>() {
            @Override
            public void eventReceived(Action action, BuildConfig buildConfig) {
                try {
                    switch (action) {
                        case ADDED:
                            collector.addBuildConfig(buildConfig);
                            break;
                        case MODIFIED:
                            collector.updateBuildConfig(buildConfig);
                            break;
                        case DELETED:
                            collector.removeBuildConfig(buildConfig);
                            break;
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process " + action + " on BuildConfig " + buildConfig + ". Reason: " + e, e);
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
            }

        });

        // lets add all the current BuildConfig's first
        BuildConfigList list = openShiftClient.buildConfigs().list();
        if (list != null) {
            List<BuildConfig> items = list.getItems();
            for (BuildConfig item : items) {
                collector.addBuildConfig(item);
            }
        }

        // now we should have the initial set of BuildConfigs to start polling on at least
        collector.start();
    }
}
