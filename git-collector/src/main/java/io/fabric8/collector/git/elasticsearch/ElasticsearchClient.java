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

import io.fabric8.collector.elasticsearch.ElasticsearchClientSupport;
import io.fabric8.collector.elasticsearch.ResultsDTO;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;

/**
 */
public class ElasticsearchClient extends ElasticsearchClientSupport implements ElasticsearchAPI {
    private ElasticsearchAPI api;

    public ElasticsearchClient() {
    }

    @Inject
    public ElasticsearchClient(@ConfigProperty(name = "ELASTICSEARCH_HOST", defaultValue = "http://elasticsearch") String elasticsearchHost,
                               @ConfigProperty(name = "ELASTICSEARCH_SERVICE_PORT") String elasticsearchPort,
                               @ConfigProperty(name = "GIT_COLLECTOR_USERNAME") String username,
                               @ConfigProperty(name = "GIT_COLLECTOR_PASSWORD") String password) {
        super(elasticsearchHost, elasticsearchPort, username, password);
    }

    @Override
    public ResultsDTO storeCommit(String namespace, String name, String sha, CommitDTO commitDto) {
        return getElasticsearchAPI().storeCommit(namespace, name, sha, commitDto);
    }

    protected ElasticsearchAPI getElasticsearchAPI() {
        if (api == null) {
            api = getElasticsearchAPIForType(ElasticsearchAPI.class);
        }
        return api;
    }
}
