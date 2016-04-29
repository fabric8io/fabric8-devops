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
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.utils.Base64Encoder;

import javax.inject.Named;
import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
                .endLifecycle().build();

        for (VolumeMount volumeMount : builder.getVolumeMounts()) {
            if (volumeMount.getName().equals("jenkins-docker-cfg")) {
                return;
            }
        }
        builder.addNewVolumeMount("/home/jenkins/.docker", "jenkins-docker-cfg", false).build();
    }

    public void on(PodSpecBuilder builder) {
        if (builder.getVolumes() != null) {
            for (Volume volume : builder.getVolumes()) {
                if (volume.getName().equals("jenkins-docker-cfg")) {
                    return;
                }
            }
        }

        builder.addNewVolume()
                .withName("jenkins-docker-cfg")
                .withNewSecret("jenkins-docker-cfg")
        .endVolume().build();
    }

    public void on(TemplateBuilder builder) {
        String version = System.getProperty("project.version");
        String versionPostfix = "";
        if (version != null && version.length() > 0 && !version.endsWith("SNAPSHOT")) {
            versionPostfix = ":" + version;
        }

        builder.addNewServiceObject()
                .withNewMetadata()
                     //The name of the service is referenced by the client image.
                .withName("jenkins")
                .addToLabels("project", "jenkins")
                .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                .withType("LoadBalancer")
                .addNewPort()
                .withName("http")
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(8080)
                .endPort()
                .addToSelector("project", "jenkins")
                .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                .build();

        builder.addNewServiceObject()
                .withNewMetadata()
                .withName("jenkins-jnlp")
                .addToLabels("project", "jenkins")
                .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                .withType("LoadBalancer")
                .addNewPort()
                .withName("agent")
                .withProtocol("TCP")
                .withPort(50000)
                .withNewTargetPort(50000)
                .endPort()
                .addToSelector("project", "jenkins")
                .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                .build();


        Map<String, String> data = new HashMap<>();
        data.put("settings.xml", new String(Base64Encoder.encode(settings.getBytes())));
        builder.addNewSecretObject()
                .withType("fabric8.io/secret-maven-settings")
                .withNewMetadata()
                .withName("jenkins-maven-settings")
                .endMetadata()
                .withData(data)
                .endSecretObject()
        .build();

        data = new HashMap<>();
        data.put("config.json", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-docker-cfg")
                .withNewMetadata()
                .withName("jenkins-docker-cfg")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();

        data = new HashMap<>();
        data.put("ssh-key", "");
        data.put("ssh-key.pub", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-git-ssh")
                .withNewMetadata()
                .withName("jenkins-git-ssh")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();

        data = new HashMap<>();
        data.put("hub", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-hub-api-token")
                .withNewMetadata()
                .withName("jenkins-hub-api-token")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();

        data = new HashMap<>();
        data.put("gpg.conf", "");
        data.put("pubring.gpg", "");
        data.put("secring.gpg", "");
        data.put("trustdb.gpg", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-release-gpg")
                .withNewMetadata()
                .withName("jenkins-release-gpg")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();

        data = new HashMap<>();
        data.put("config", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-ssh-config")
                .withNewMetadata()
                .withName("jenkins-ssh-config")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();

        data = new HashMap<>();
        data.put("idrsa", "");
        builder.addNewSecretObject()
                .withType("fabric8.io/jenkins-master-ssh")
                .withNewMetadata()
                .withName("jenkins-master-ssh")
                .endMetadata()
                .withData(data)
                .endSecretObject()
                .build();
    }

    private final String settings = "<settings>\n" +
            "  <!--This sends everything else to /public -->\n" +
            "  <mirrors>\n" +
            "    <mirror>\n" +
            "      <id>nexus</id>\n" +
            "      <mirrorOf>external:*</mirrorOf>\n" +
            "      <url>http://nexus/content/groups/public</url>\n" +
            "    </mirror>\n" +
            "  </mirrors>\n" +
            "\n" +
            "  <!-- lets disable the download progress indicator that fills up logs -->\n" +
            "  <interactiveMode>false</interactiveMode>\n" +
            "\n" +
            "  <servers>\n" +
            "    <server>\n" +
            "      <id>local-nexus</id>\n" +
            "      <username>admin</username>\n" +
            "      <password>admin123</password>\n" +
            "    </server>\n" +
            "    <server>\n" +
            "      <id>nexus</id>\n" +
            "      <username>admin</username>\n" +
            "      <password>admin123</password>\n" +
            "    </server>\n" +
            "    <server>\n" +
            "      <id>oss-sonatype-staging</id>\n" +
            "      <username></username>\n" +
            "      <password></password>\n" +
            "    </server>\n" +
            "  </servers>\n" +
            "\n" +
            "  <profiles>\n" +
            "    <profile>\n" +
            "      <id>nexus</id>\n" +
            "      <properties>\n" +
            "        <altDeploymentRepository>local-nexus::default::http://nexus/content/repositories/snapshots/</altDeploymentRepository>\n" +
            "        <altReleaseDeploymentRepository>local-nexus::default::http://nexus/content/repositories/staging/</altReleaseDeploymentRepository>\n" +
            "        <altSnapshotDeploymentRepository>local-nexus::default::http://nexus/content/repositories/snapshots/</altSnapshotDeploymentRepository>\n" +
            "      </properties>\n" +
            "      <repositories>\n" +
            "        <repository>\n" +
            "          <id>central</id>\n" +
            "          <url>http://central</url>\n" +
            "          <releases><enabled>true</enabled></releases>\n" +
            "          <snapshots><enabled>true</enabled></snapshots>\n" +
            "        </repository>\n" +
            "      </repositories>\n" +
            "      <pluginRepositories>\n" +
            "        <pluginRepository>\n" +
            "          <id>central</id>\n" +
            "          <url>http://central</url>\n" +
            "          <releases><enabled>true</enabled></releases>\n" +
            "          <snapshots><enabled>true</enabled></snapshots>\n" +
            "        </pluginRepository>\n" +
            "      </pluginRepositories>\n" +
            "    </profile>\n" +
            "    <profile>\n" +
            "      <id>release</id>\n" +
            "      <properties>\n" +
            "        <gpg.executable>gpg</gpg.executable>\n" +
            "        <gpg.passphrase>mysecretpassphrase</gpg.passphrase>\n" +
            "      </properties>\n" +
            "    </profile>\n" +
            "  </profiles>\n" +
            "  <activeProfiles>\n" +
            "    <!--make the profile active all the time -->\n" +
            "    <activeProfile>nexus</activeProfile>\n" +
            "  </activeProfiles>\n" +
            "</settings>";
}
