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

import io.fabric8.utils.Millis;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static io.fabric8.systests.SeleniumTests.logWarn;
import static org.junit.Assert.fail;

/**
 */
public class ProjectsPage extends PageSupport {

    public ProjectsPage(WebDriverFacade driverFacade) {
        super(driverFacade);
    }

    public void login() {

        WebDriverFacade facade = getFacade();
        WebDriver driver = getDriver();

        ConsoleTests.waitUntilLoggedIn(facade);

        String startUrl = driver.getCurrentUrl();
        String expectedUrl = relativeUrl(startUrl, "/kubernetes", "/kubernetes/buildConfigs");

        By by = By.linkText("Projects");
        facade.untilLinkClickedLoop(by, expectedUrl);


        final By signInBy = By.linkText("Sign In");
        final By createProjectBy = By.partialLinkText("Create Project");

        facade.untilOneOf(signInBy, createProjectBy);

        WebElement signIn = facade.findOptionalElement(signInBy);
        if (signIn != null && signIn.isDisplayed()) {
            System.out.println("Waiting for signin button to be clicked at " + driver.getCurrentUrl());
            facade.untilLinkClicked(signInBy);
            signIntoGogs();
        } else {
            System.out.println("Sign in button not present at " + driver.getCurrentUrl());
        }

        System.out.println("Now waiting for the button: " + createProjectBy + " at " +  driver.getCurrentUrl());
        facade.untilLinkClicked(createProjectBy);
    }

    /**
     * Returns a new URL from the given url trimming the `trimPath` and adding the `newPath`
     */
    protected String relativeUrl(String url, String trimPath, String newPath) {
        int idx = url.indexOf(trimPath);
        if (idx < 0) {
            fail("The URL `" + url + "` does not include path `" + trimPath + "`");
        }
        return url.substring(0, idx) + newPath;
    }

    /**
     * Once on the sign in page lets sign in
     */
    protected void signIntoGogs() {
        getFacade().form().
                clearAndSendKeys(By.id("gitUsername"), "gogsadmin").
                clearAndSendKeys(By.id("gitPassword"), "RedHat$1").
                clearAndSendKeys(By.id("gitEmail"), "james.strachan@gmail.com").
                submitButton(By.xpath("//button[@ng-click='doLogin()']")).
                submit();
    }
}
