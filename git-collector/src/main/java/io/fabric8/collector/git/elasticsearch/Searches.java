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
package io.fabric8.collector.git.elasticsearch;

import io.fabric8.collector.elasticsearch.FilterDTO;
import io.fabric8.collector.elasticsearch.SearchDTO;

/**
 */
public class Searches {
    public static SearchDTO createMinMaxGitCommitSearch(String namespace, String app, String branch, boolean ascending) {
        SearchDTO search = new SearchDTO();
        FilterDTO filter = search.getFilter();
        filter.bool().mustTerm("namespace", namespace);
        filter.bool().mustTerm("app", app);
        filter.bool().mustTerm("branch", branch);
        search.setSize(1L);
        search.addSort("commit_time", ascending);
        return search;
    }
}
