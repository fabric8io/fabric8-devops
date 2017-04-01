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
package io.fabric8.apps.kibana;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.collector.elasticsearch.ElasticsearchClient;
import io.fabric8.collector.elasticsearch.SearchDTO;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import io.fabric8.utils.jaxrs.JsonHelper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * A helper application to dump the contents of the <code>.kibana</code> index to the file
 * system so we can store it into a docker image and automatically populate an Elasticsearch
 * cluster with the default fabric8 searches, visualisations and dashboards
 */
public class ExportKibanaObjects {
    private static String index = ".kibana";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Arguments: elasticsearchURL [outputFolder]");
            System.exit(1);
        }
        String elasticsearchUrl = args[0];
        String outputFolder = "target/kibana-objects";
        if (args.length > 1) {
            outputFolder = args[1];
        }
        File dir = new File(outputFolder);
        try {
            ElasticsearchClient client = new ElasticsearchClient(elasticsearchUrl);
            exportKibanaObjects(client, dir);
        } catch (IOException e) {
            System.out.println("Failed: " + e);
            e.printStackTrace();
        }
    }

    public static void exportKibanaObjects(ElasticsearchClient es, File dir) throws IOException {
        exportKibanaTemplates(es, new File(dir, "_template"));

        String[] types = {"index-pattern", "search", "dashboard", "config", "visualization"};
        for (String type : types) {
            File typeDir = new File(dir, type);
            exportKibanaObjectsForType(es, type, typeDir);
        }
    }

    public static void exportKibanaObjectsForType(ElasticsearchClient es, String type, File dir) throws IOException {
        System.out.println("Finding kibana objects of type: " + type);
        dir.mkdirs();
        SearchDTO search = new SearchDTO();
        search.matchAll();
        ObjectNode results = es.search(index, type, search);

        JsonNode hitArray = results.path("hits").path("hits");
        if (hitArray.isArray()) {
            for (int i = 0, size = hitArray.size(); i < size; i++) {
                JsonNode item = hitArray.get(i);
                if (item != null) {
                    JsonNode idNode = item.get("_id");
                    String id = idNode.asText();
                    JsonNode source = item.get("_source");
                    if (Strings.isNotBlank(id) && source.isObject()) {
                        File file = new File(dir, id + ".json");
                        String json = JsonHelper.toJson(source);
                        Files.writeToFile(file, json.getBytes());
                    }
                }
            }
        }
    }

    public static void exportKibanaTemplates(ElasticsearchClient es, File dir) throws IOException {
        System.out.println("Finding kibana templates");
        dir.mkdirs();
        ObjectNode results = es.getIndex("_template", "*");

        Iterator<Map.Entry<String, JsonNode>> iter = results.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            String id = entry.getKey();
            JsonNode value = entry.getValue();
            File file = new File(dir, id + ".json");
            String json = JsonHelper.toJson(value);
            Files.writeToFile(file, json.getBytes());
        }
    }
}
