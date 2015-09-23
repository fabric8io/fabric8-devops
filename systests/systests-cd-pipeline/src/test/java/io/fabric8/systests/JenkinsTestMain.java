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
package io.fabric8.systests;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import io.fabric8.selenium.SeleniumTests;

import java.util.Map;
import java.util.Set;

/**
 * A simple class to test asserts on jenkins
 */
public class JenkinsTestMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: jobName [jenkinsServerUrl]");
            return;
        }
        String job = args[0];
        String jenkinsUrl = "http://jenkins.vagrant.f8/";
        if (args.length > 1) {
            jenkinsUrl = args[1];
        }

        try {
            JenkinsServer jenkins = JenkinsAsserts.createJenkinsServer(jenkinsUrl);

            Map<String, Job> jobs = jenkins.getJobs();
            Set<Map.Entry<String, Job>> entries = jobs.entrySet();
            for (Map.Entry<String, Job> entry : entries) {
                System.out.println("Job " + entry.getKey() + " = " + entry.getValue());
            }

            JenkinsAsserts.assertJobLastBuildIsSuccessful(jenkins, job);
        } catch (Exception e) {
            SeleniumTests.logError(e.getMessage(), e);
        }

    }

}
