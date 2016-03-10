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
package io.fabric8.collector.elasticsearch;

import io.fabric8.utils.Files;
import io.fabric8.utils.Ports;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class EmbeddedElasticsearchTestSupport {
    protected Node elasticsearchNode;
    protected String elasticsearchPort = "1234";
    protected String elasticsearchNodeName = "TestNode";
    protected static Set<Integer> usedPorts = new HashSet<>();

    public static File getBaseDir() {
        String dirName = System.getProperty("basedir", ".");
        return new File(dirName);
    }

    @Before
    public void init() throws Exception {
        File esHomeDir = new File(getBaseDir(), "target/elasticsearch/" + elasticsearchNodeName);
        Files.recursiveDelete(esHomeDir);
        File esDataDir = new File(esHomeDir, "data");
        esDataDir.mkdirs();
        File esLogDir = new File(esHomeDir, "logs");
        esLogDir.mkdirs();

        // lets create a dynamic port just in case
        int freePort = Ports.findFreeLocalPort(usedPorts, 9200, 50000, true);
        if (freePort < 100) {
            freePort = 9200;
        }
        elasticsearchPort = "" + freePort;

        NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder();
        Settings.Builder settings = nodeBuilder.getSettings();
        settings.put("http.enabled", "true").
                put("node.name", elasticsearchNodeName).
                put("http.port", elasticsearchPort).
                put("path.home", esHomeDir.toString()).
                put("path.data", esDataDir.toString()).
                put("path.logs", esLogDir.toString());

        elasticsearchNode = nodeBuilder
                .local(true)
                .settings(settings.build())
                .node();

        elasticsearchNode.start();
    }

    @After
    public void close() throws Exception {
        elasticsearchNode.close();
    }
}
