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
package io.fabric8.collector.git.elasticsearch;

import io.fabric8.collector.elasticsearch.ElasticsearchAPI;
import io.fabric8.collector.elasticsearch.ResultsDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Represents the REST API for talking to Hubot
 */
@Produces("application/json")
public interface GitElasticsearchAPI extends ElasticsearchAPI {
    @PUT
    @Path("/{index}/{type}/{sha}")
    @Consumes("application/json")
    ResultsDTO storeCommit(@PathParam("index") String index,
                           @PathParam("type") String type,
                           @PathParam("sha") String sha,
                           CommitDTO commitDto);

}
