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
package io.fabric8.collector.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.utils.Function;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.ExceptionResponseMapper;
import io.fabric8.utils.jaxrs.JAXRSClients;
import io.fabric8.utils.jaxrs.JsonHelper;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A base client for communicating with Elasticsearch
 */
public class ElasticsearchClient implements ElasticsearchAPI {
    public static final String ELASTICSEARCH_SERVICE_NAME = "elasticsearch";

    private static final transient Logger LOG = LoggerFactory.getLogger(ElasticsearchClient.class);
    private String username;
    private String password;
    private String elasticsearchPort;
    private String elasticsearchUrl;
    private boolean initalised;
    private ElasticsearchAPI api;

    public ElasticsearchClient() {
        this(null, null);
    }

    public ElasticsearchClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public ElasticsearchClient(String elasticsearchHost, String elasticsearchPort, String username, String password) {
        this(username, password);
        this.elasticsearchPort = elasticsearchPort;
        this.elasticsearchUrl = Strings.stripSuffix(Strings.stripSuffix(elasticsearchHost, ":"), "/");
        if (Strings.isNotBlank(elasticsearchPort)) {
            elasticsearchUrl += ":" + elasticsearchPort;
        }
        if (!elasticsearchUrl.endsWith("/")) {
            elasticsearchUrl = elasticsearchUrl + "/";
        }
    }

    public ElasticsearchClient(String elasticsearchUrl) {
        this.elasticsearchUrl = elasticsearchUrl;
    }

    public static List<Object> createProviders() {
        ObjectMapper objectMapper = JsonHelper.createObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return createProviders(objectMapper);
    }

    // TODO we can move to the helper function in fabric8-utils when the next release is out!
    public static List<Object> createProviders(ObjectMapper objectMapper) {
        ArrayList providers = new ArrayList();
        Annotations[] annotationsToUse = JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS;
        providers.add(new JacksonJaxbJsonProvider(objectMapper, annotationsToUse));
        providers.add(new ExceptionResponseMapper());
        return providers;
    }

    public String getElasticsearchUrl() {
        if (!initalised) {
            initalised = true;
            if (Strings.isNotBlank(elasticsearchUrl)) {
                LOG.info("Communicating with Elasticsearch at address: " + elasticsearchUrl);
            } else {
                //LOG.warn("No kubernetes service found for " + ELASTICSEARCH_SERVICE_NAME);
                elasticsearchUrl = "http://" + ELASTICSEARCH_SERVICE_NAME;
                if (Strings.isNullOrBlank(elasticsearchPort)) {
                    elasticsearchUrl += ":" + elasticsearchPort;
                }
                if (!elasticsearchUrl.endsWith("/")) {
                    elasticsearchUrl = elasticsearchUrl + "/";
                }
            }
        }
        return elasticsearchUrl;
    }

    public ObjectNode search(String index, String type, SearchDTO search) {
        return getElasticsearchAPI().search(index, type, search);
    }

    public ObjectNode getIndex(String index) {
        return getElasticsearchAPI().getIndex(index);
    }

    public ObjectNode getIndex(String index, String type) {
        return getElasticsearchAPI().getIndex(index, type);
    }

    public ObjectNode createIndex(String index, ObjectNode metadata) {
        return getElasticsearchAPI().createIndex(index, metadata);
    }

    public ObjectNode updateIndex(String index, ObjectNode metadata) {
        return getElasticsearchAPI().updateIndex(index, metadata);
    }

    public ObjectNode createIndexMapping(String index, String type, ObjectNode metadata) {
        return getElasticsearchAPI().createIndexMapping(index, type, metadata);
    }

    public ObjectNode updateIndexMapping(String index, String type, ObjectNode metadata) {
        return getElasticsearchAPI().updateIndexMapping(index, type, metadata);
    }

    public ObjectNode getIndexMapping(String index, String type) {
        return getElasticsearchAPI().getIndexMapping(index, type);
    }

    public ObjectNode createIndexIfMissing(final String index, final String type, Function<ObjectNode, Boolean> updater) {
        ObjectNode metadata = JAXRSClients.handle404ByReturningNull(new Callable<ObjectNode>() {
            @Override
            public ObjectNode call() throws Exception {
                return getIndex(index);
            }
        });
        boolean create = false;
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        if (metadata == null) {
            create = true;
            metadata = nodeFactory.objectNode();
/*
            ObjectNode properties = JsonNodes.setObjects(metadata, index, "mappings", type, "properties");
            if (properties == null) {
                LOG.warn("Failed to create object path!");
            }
*/
        }
        if (!updater.apply(metadata)) {
            return null;
        }
        if (create) {
            return getElasticsearchAPI().createIndex(index, metadata);
        } else {
            return null;
        }
    }

    public ObjectNode createIndexMappingIfMissing(final String index, final String type, Function<ObjectNode, Boolean> updater) {
        ObjectNode metadata = JAXRSClients.handle404ByReturningNull(new Callable<ObjectNode>() {
            @Override
            public ObjectNode call() throws Exception {
                return getIndexMapping(index, type);
            }
        });
        boolean create = false;
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        if (metadata == null) {
            create = true;
            metadata = nodeFactory.objectNode();
        }
        ObjectNode properties = null;
        JsonNode propertiesNode = metadata.path(index).path("mappings").path(type).path("properties");
        if (propertiesNode.isObject()) {
            properties = (ObjectNode) propertiesNode;
        }
        else {
            create = true;
            properties = JsonNodes.setObjects(metadata, index, "mappings", type, "properties");
        }
        if (properties == null) {
            LOG.warn("Failed to create object path!");
        }
        if (!updater.apply(properties)) {
            return null;
        }
        if (create) {
            ObjectNode typeNode = (ObjectNode) metadata.path(index).path("mappings").path(type);
            return getElasticsearchAPI().updateIndexMapping(index, type, typeNode);
        } else {
            return null;
        }
    }

    protected ElasticsearchAPI getElasticsearchAPI() {
        if (api == null) {
            api = getElasticsearchAPIForType(ElasticsearchAPI.class);
        }
        return api;
    }

    /**
     * Returns a REST client for the given API
     */
    protected <T> T getElasticsearchAPIForType(Class<T> clazz) {
        T api = null;
        String url = getElasticsearchUrl();
        if (Strings.isNotBlank(url)) {
            api = createWebClient(clazz, url);
        }
        Objects.notNull(api, "No kubernetes service found for " + ELASTICSEARCH_SERVICE_NAME);
        return api;
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    protected <T> T createWebClient(Class<T> clientType, String url) {
        LOG.info("Creating client for " + clientType.getName() + " on URL: " + url);
        List<Object> providers = createProviders();
        WebClient webClient = WebClient.create(url, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, username, password);
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

}
