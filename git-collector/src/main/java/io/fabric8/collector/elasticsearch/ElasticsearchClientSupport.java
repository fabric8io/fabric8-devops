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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.ExceptionResponseMapper;
import io.fabric8.utils.cxf.JsonHelper;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.createProviders;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A base client for communicating with Elasticsearch
 */
public abstract class ElasticsearchClientSupport {
    public static final String ELASTICSEARCH_SERVICE_NAME = "elasticsearch";

    private static final transient Logger LOG = LoggerFactory.getLogger(ElasticsearchClientSupport.class);
    private final String username;
    private final String password;
    private String elasticsearchUrl;
    private boolean initalised;

    public ElasticsearchClientSupport() {
        this(null, null);
    }

    public ElasticsearchClientSupport(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public ElasticsearchClientSupport(String elasticsearchHost, String elasticsearchPort, String username, String password) {
        this(username, password);
        this.elasticsearchUrl = Strings.stripSuffix(Strings.stripSuffix(elasticsearchHost, ":"), "/");
        if (Strings.isNotBlank(elasticsearchPort)) {
            elasticsearchUrl += ":" + elasticsearchPort + "/";
        }
    }

    public String getElasticsearchUrl() {
        if (!initalised) {
            initalised = true;
            if (Strings.isNotBlank(elasticsearchUrl)) {
                LOG.info("Communicating with Elasticsearch at address: " + elasticsearchUrl);
            } else {
                //LOG.warn("No kubernetes service found for " + ELASTICSEARCH_SERVICE_NAME);
                elasticsearchUrl = "http://" + ELASTICSEARCH_SERVICE_NAME + ":9200";
            }
        }
        return elasticsearchUrl;
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
}
