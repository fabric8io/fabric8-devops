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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a <code>filter</code> expression in an Elasticsearch query
 */
public class FilterDTO extends DTOSupport {
    private BoolDTO bool;
    private Map<String, String> matchAll;

    public BoolDTO bool() {
        if (bool == null) {
            bool = new BoolDTO();
        }
        return bool;
    }

    public BoolDTO getBool() {
        return bool;
    }

    public void setBool(BoolDTO bool) {
        this.bool = bool;
    }

    public void matchAll() {
        matchAll = new HashMap();
    }
}