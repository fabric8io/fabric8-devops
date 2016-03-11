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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 */
public class JsonNodes {
    /**
     * Sets a property on a node
     */
    public static boolean set(JsonNode node, String name, JsonNode value) {
        if (node != null && node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.set(name, value);
            return true;
        }
        return false;
    }

    /**
     * Sets a property on a node
     */
    public static boolean set(JsonNode node, String name, String text) {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        return set(node, name, nodeFactory.textNode(text));
    }

    /**
     * Creates nested objects if they don't exist on the given paths.
     *
     * @returns the last object or null if it could not be created
     */
    public static ObjectNode setObjects(JsonNode node, String... paths) {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        JsonNode iter = node;
        for (String path : paths) {
            if (!iter.isObject()) {
                return null;
            }
            ObjectNode object = (ObjectNode) iter;
            iter = object.get(path);
            if (iter == null || !iter.isObject()) {
                iter = nodeFactory.objectNode();
                object.set(path, iter);
            }
        }
        return (ObjectNode) iter;
    }
}
