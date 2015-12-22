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
package io.fabric8.forge.systest.support;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;

import java.io.File;
import java.util.concurrent.Future;

/**
 */
public class Furnaces {
    public static <T> T withFurnace(FurnaceCallback<T> callback) throws Exception {
        Furnace furnace = startFurnace();
        try {
            return callback.invoke(furnace);
        } finally {
            furnace.stop();
        }
    }

    static Furnace startFurnace() throws Exception {
        // Create a Furnace instance. NOTE: This must be called only once
        Furnace furnace = FurnaceFactory.getInstance();

        // Add repository containing addons specified in pom.xml
        furnace.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target/addon-repository"));

        // Start Furnace in another thread
        Future<Furnace> future = furnace.startAsync();

        // Wait until Furnace is started and return
        return future.get();
    }
}
