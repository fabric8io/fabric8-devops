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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static org.junit.Assert.assertNotNull;

/**
 * Helper methods for using Selenium tests with Kubernetes
 */
public class SeleniumTests {
    private static final transient Logger LOG = LoggerFactory.getLogger(SeleniumTests.class);
    public static final String WAIT_AFTER_SELENIUM_PROPERTY = "fabric8.selenium.waitAfterTest";

    public static <T> T assertWebDriverForService(KubernetesClient client, String namespace, String serviceName,  Function<WebDriverFacade, T> block) throws Exception {
        WebDriver driver = createWebDriverForService(client, namespace, serviceName);
        try {
            WebDriverFacade facade = new WebDriverFacade(driver);
            T apply = block.apply(facade);

            String property = System.getProperty(WAIT_AFTER_SELENIUM_PROPERTY);
            if (property != null) {
                long millis = 0;
                try {
                    millis = Long.parseLong(property);
                } catch (NumberFormatException e) {
                    LOG.warn("System property " + WAIT_AFTER_SELENIUM_PROPERTY + " is not a long value: " + property + ". " + e, e);
                }
                if (millis > 0) {
                    logInfo("Sleeping for " + millis + " millis before tearning down the test case");
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            return apply;
        } catch (Exception e) {
            logError("Failed with exception: ", e);
            throw e;
        } finally {
            driver.quit();
        }
    }

    public static void logError(String message, Throwable e) {
        System.out.println("ERROR: " + message + e);
        e.printStackTrace();
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            logError("Caused by: ", cause);
        }
    }

    public static void logWait(String message) {
        System.out.println("WAITING: " + message);
    }

    public static void logInput(String message) {
        System.out.println("INPUT: " + message);
    }

    public static void logClick(String message) {
        System.out.println("CLICK: " + message);
    }

    public static void logSubmit(String message) {
        System.out.println("SUBMIT: " + message);
    }

    public static void logInfo(String message) {
        System.out.println("INFO: " + message);
    }

    public static void logWarn(String message) {
        System.out.println("WARN: " + message);
    }

    public static void logWarn(String message, Throwable e) {
        System.out.println("WARN: " + message + e);
        e.printStackTrace();
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            logWarn("Caused by: ", cause);
        }
    }

    public static WebDriver createWebDriverForService(KubernetesClient client, String namespace, String serviceName) {
        String url = KubernetesHelper.getServiceURL(client, serviceName, namespace, "http", true);
        assertNotNull("No external Service URL could be found for namespace: " + namespace + " and name: " + serviceName, url);
        logInfo("Connecting to service " + serviceName + " at " + url);

        return createWebDriver(client, url);
    }

    public static WebDriver createWebDriver(KubernetesClient client, String url) {
        // TODO let a system property / env var be used to switch driver...
        WebDriver answer = new ChromeDriver();
        //WebDriver answer = new PhantomJSDriver();
        //WebDriver answer = new HtmlUnitDriver();
        if (Strings.isNotBlank(url)) {
            answer.navigate().to(url);
        }
        return answer;
    }

    public static void logPageLocation(WebDriver d) {
        String currentUrl = d.getCurrentUrl();
        String title = d.getTitle();
        logInfo("At " + title + " : " + currentUrl);
    }
}
