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

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 */
public interface ElasticsearchAPI {
    @POST
    @Path("/{index}/{type}/_search")
    @Consumes("application/json")
    ObjectNode search(@PathParam("index") String index,
                      @PathParam("type") String type,
                      SearchDTO search);

    @GET
    @Path("/{index}")
    @Consumes("application/json")
    ObjectNode getIndex(@PathParam("index") String index);

    @GET
    @Path("/{index}/{type}")
    @Consumes("application/json")
    ObjectNode getIndex(@PathParam("index") String index, @PathParam("type") String type);

    @POST
    @Path("/{index}")
    @Consumes("application/json")
    ObjectNode createIndex(@PathParam("index") String index, ObjectNode metadata);

    @PUT
    @Path("/{index}")
    @Consumes("application/json")
    ObjectNode updateIndex(@PathParam("index") String index, ObjectNode metadata);

    @GET
    @Path("/{index}/_mapping/{type}")
    @Consumes("application/json")
    ObjectNode getIndexMapping(@PathParam("index") String index, @PathParam("type") String type);

    @POST
    @Path("/{index}/_mapping/{type}")
    @Consumes("application/json")
    ObjectNode createIndexMapping(@PathParam("index") String index, @PathParam("type") String type, ObjectNode metadata);

    @PUT
    @Path("/{index}/_mapping/{type}")
    @Consumes("application/json")
    ObjectNode updateIndexMapping(@PathParam("index") String index, @PathParam("type") String type, ObjectNode metadata);
}
