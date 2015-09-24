/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.systests;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.assertions.KubernetesAssert;
import io.fabric8.kubernetes.assertions.KubernetesNamespaceAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ClientNonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import io.fabric8.utils.Millis;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jolokia.client.J4pClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static io.fabric8.jolokia.assertions.Assertions.assertThat;

/**
 * System test for the <a href="http://fabric8.io/guide/cdelivery.html">CD Pipeline</a>
 */
@RunWith(Arquillian.class)
public class ChatLetsChatKT {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @ArquillianResource
    JolokiaClients jolokiaClients;

    String fabric8Console = "fabric8";
    String letschat = "letschat";
    String hubotLetsChat = "hubot-letschat";
    String hubotNotifier = "hubot-notifier";

    String hubotStartupText = "Now watching services, pods";

    @Test
    public void testChat() throws Exception {
        String namespace = session.getNamespace();
        final KubernetesNamespaceAssert asserts = assertThat(client, namespace);

        asserts.replicationController(letschat).isNotNull();
        asserts.replicationController(hubotLetsChat).isNotNull();
        asserts.replicationController(hubotNotifier).isNotNull();
        asserts.replicationController(fabric8Console).isNotNull();


        Asserts.assertForPeriod(Millis.minutes(10), new Block() {
            @Override
            public void invoke() throws Exception {
                asserts.podsForReplicationController(hubotNotifier).logs().containsText(hubotStartupText);
            }
        });

        Asserts.assertForPeriod(Millis.minutes(1), new Block() {
            @Override
            public void invoke() throws Exception {
                asserts.podsForReplicationController(letschat).logs(letschat).doesNotContainText("Exception", "Error");
                asserts.podsForReplicationController(hubotNotifier).logs().afterText(hubotStartupText).doesNotContainText("Exception");
            }
        });

        // now lets scale up the console pods to force the notifier to send a message
        client.replicationControllers().inNamespace(namespace).withName(fabric8Console).scale(2);

        Asserts.assertWaitFor(Millis.minutes(1), new Block() {
            @Override
            public void invoke() throws Exception {
                asserts.podsForReplicationController(hubotNotifier).logs().afterText(hubotStartupText).containsText("added pod ");
            }
        });

        asserts.podsForReplicationController(letschat).logs(letschat).doesNotContainText("Exception", "Error");
        asserts.podsForReplicationController(hubotNotifier).logs().afterText(hubotStartupText).doesNotContainText("Exception");
    }

}
