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
package io.fabric8.forge.systest;

import io.fabric8.forge.systest.support.FurnaceCallback;
import io.fabric8.forge.systest.support.Furnaces;
import io.fabric8.utils.Files;
import org.jboss.forge.furnace.Furnace;
import org.junit.Test;

import java.io.File;

/**
 */
public class PopulateMavenRepositoryTest {
    protected String baseDir = System.getProperty("basedir", ".");

    @Test
    public void testPopulateMavenRepo() throws Exception {
        // lets point to a local maven repo
        File localMavenRepo = new File(baseDir, "target/localMavenRepo");
        localMavenRepo.mkdirs();
        System.setProperty("maven.repo.local", localMavenRepo.getAbsolutePath());

        Furnaces.withFurnace(new FurnaceCallback<String>() {

            @Override
            public String invoke(Furnace furnace) throws Exception {
                createProjects(furnace);
                return null;
            }
        });
    }

    protected void createProjects(Furnace furnace) throws Exception {
        File projectsOutputFolder = new File(baseDir, "target/createdProjects");
        Files.recursiveDelete(projectsOutputFolder);

        ProjectGenerator generator = new ProjectGenerator(furnace, projectsOutputFolder);
        String[] archetypes = {"cdi-camel-archetype", "cdi-cxf-archetype"};
        for (String archetype : archetypes) {
            generator.createProject(archetype);
        }
    }

}
