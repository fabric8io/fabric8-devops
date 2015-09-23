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
package io.fabric8.systests;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.forge.NewProjectFormData;
import io.fabric8.forge.ProjectsPage;
import io.fabric8.kubernetes.assertions.KubernetesNamespaceAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.selenium.SeleniumTests;
import io.fabric8.selenium.WebDriverFacade;
import io.fabric8.selenium.support.NameGenerator;
import io.fabric8.selenium.support.Versions;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import io.fabric8.utils.Millis;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Function;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;

/**
 * System test for the <a href="http://fabric8.io/guide/cdelivery.html">CD Pipeline</a>
 */
@RunWith(Arquillian.class)
public class CDPipelineKT {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @ArquillianResource
    JolokiaClients jolokiaClients;

    String jenkinsName = "jenkins";
    String nexusName = "nexus";
    String gogsName = "nexus";
    String fabric8Console = "fabric8";
    String fabric8Forge = "fabric8-forge";

    @Test
    public void testCreateCamelCDIProjectFromArchetype() throws Exception {
        final String namespace = session.getNamespace();
        final KubernetesNamespaceAssert asserts = assertThat(client, namespace);

        asserts.replicationController(jenkinsName).isNotNull();
        asserts.replicationController(nexusName).isNotNull();
        asserts.replicationController(gogsName).isNotNull();
        asserts.replicationController(fabric8Forge).isNotNull();
        asserts.replicationController(fabric8Console).isNotNull();

        asserts.podsForService(fabric8Console).runningStatus().assertSize().isGreaterThan(0);

        Asserts.assertWaitFor(Millis.minutes(10), new Block() {
            @Override
            public void invoke() throws Exception {
                asserts.podsForReplicationController(fabric8Forge).logs().containsText("oejs.Server:main: Started");
            }
        });

        SeleniumTests.assertWebDriverForService(client, namespace, fabric8Console, new Function<WebDriverFacade, String>() {
            @Override
            public String apply(WebDriverFacade facade) {
                ProjectsPage projects = new ProjectsPage(facade, namespace);
                String projectName = "p" + NameGenerator.generateName();
                String archetypeFilter = "io.fabric8.archetypes:java-camel-cdi-archetype:" + Versions.getVersion("fabric8.archetypes.release.version");
                NewProjectFormData projectData = new NewProjectFormData(projectName, archetypeFilter, "maven/CanaryReleaseStageAndApprovePromote.groovy");
                projects.createProject(projectData);
                return null;
            }
        });
    }

}
