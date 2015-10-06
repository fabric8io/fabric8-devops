/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apps.jenkins;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.TemplateBuilder;

import javax.inject.Named;

@KubernetesModelProcessor
public class JenkinsModelProcessor {

    @Named("jenkins")
    public void on(ContainerBuilder builder) {
        builder.withNewLifecycle()
                .withNewPostStart()
                .withNewExec()
                .addToCommand("/root/postStart.sh")
                .endExec()
                .endPostStart()
                .endLifecycle()
                .build();
    }

    public void on(TemplateBuilder builder) {
        builder.addNewReplicationControllerObject()
                .withNewMetadata()
                    .withName("jenkins-swarm-client")
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .addToSelector("provider", "fabric8")
                    .addToSelector("component", "jenkins-swarm-client")
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("provider", "fabric8")
                            .addToLabels("component", "jenkins-swarm-client")
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("jenkins-swarm-client")
                                .withImage("fabric8/jenkins-swarm-client")
                                .withNewSecurityContext()
                                    .withPrivileged(true)
                                .endSecurityContext()
                                .addNewVolumeMount()
                                    .withName("jenkins-workspace")
                                    .withMountPath("/var/jenkins_home/workspace")
                                    .withReadOnly(false)
                                .endVolumeMount()
                                .addNewPort()
                                    .withName("http")
                                    .withContainerPort(8080)
                                .endPort()
                            .endContainer()
                            .addNewVolume()
                                .withName("jenkins-workspace")
                                .withNewHostPath("/var/jenkins_home/workspace")
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .and()

                .addNewServiceObject()
                .withNewMetadata()
                    .withName("jenkins-swarm-client")
                        .addToLabels("component", "jenkins-swarm-client")
                        .addToLabels("provider", "fabric8")
                    .endMetadata()
                .withNewSpec()
                .withType("LoadBalancer")
                .addNewPort()
                    .withProtocol("TCP")
                    .withPort(50000)
                    .withNewTargetPort(50000)
                .endPort()
                    .addToSelector("component", "jenkins")
                    .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()

                .build();

    }
}
