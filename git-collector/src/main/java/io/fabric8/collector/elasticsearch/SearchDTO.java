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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a search
 */
public class SearchDTO extends DTOSupport {
    private FilterDTO filter = new FilterDTO();
    private List<Map<String, SortDTO>> sort;
    private Long size;


    public void matchAll() {
        filter.matchAll();
    }

    public void addSort(String name, boolean ascending) {
        SortDTO sortDto = ascending ? SortDTO.createAscending() : SortDTO.createDescending();
        addSort(name, sortDto);
    }

    public void addSort(String name, SortDTO sortDto) {
        if (sort == null) {
            sort = new ArrayList<>();
        }
        Map<String, SortDTO> map = new LinkedHashMap<>();
        map.put(name, sortDto);
        sort.add(map);
    }

    public FilterDTO getFilter() {
        return filter;
    }

    public void setFilter(FilterDTO filter) {
        this.filter = filter;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<Map<String, SortDTO>> getSort() {
        return sort;
    }

    public void setSort(List<Map<String, SortDTO>> sort) {
        this.sort = sort;
    }
}
