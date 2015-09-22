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

import static io.fabric8.systests.SeleniumTests.logInfo;
import static io.fabric8.systests.SeleniumTests.logWait;
import static org.junit.Assert.fail;

/**
 */
public class ProjectsPage extends PageSupport {
    private final By signInBy = By.linkText("Sign In");
    private final By createProjectBy = By.partialLinkText("Create Project");
    private final WebDriverFacade driverFacade;
    private final String namespace;


    public ProjectsPage(WebDriverFacade driverFacade, String namespace) {
        super(driverFacade);
        this.driverFacade = driverFacade;
        this.namespace = namespace;
    }

    public void loginAndGoToProjectsPage() {
        WebDriverFacade facade = getFacade();
        WebDriver driver = getDriver();

        ConsoleTests.waitUntilLoggedIn(facade, namespace);

        String startUrl = driver.getCurrentUrl();
        String expectedUrl = relativeUrl(startUrl, "/kubernetes", "/kubernetes/buildConfigs");

        By by = By.linkText("Projects");
        facade.untilLinkClickedLoop(by, expectedUrl);

        facade.untilOneOf(signInBy, createProjectBy);

        WebElement signIn = facade.findOptionalElement(signInBy);
        if (signIn != null && signIn.isDisplayed()) {
            logInfo("Waiting for signin button to be clicked at " + driver.getCurrentUrl());
            facade.untilLinkClicked(signInBy);
            signIntoGogs();
        } else {
            logInfo("Sign in button not present at " + driver.getCurrentUrl());
        }

        logWait("button: " + createProjectBy + " at " + driver.getCurrentUrl());
        facade.untilIsEnabled(createProjectBy);
    }

    /**
     * Creates a new project using the create projects wizard
     */
    public void createProject(NewProjectFormData form) {
        loginAndGoToProjectsPage();

        WebDriverFacade facade = getFacade();
        WebDriver driver = getDriver();
        facade.untilLinkClicked(createProjectBy);

        By nextButton = By.xpath("//button[@ng-click='execute()']");


        // it can take a while to load pages in the wizard to lets increase the wait time lots! :)
        facade.setDefaultTimeoutInSeconds(60 * 3);

        facade.form().
                clearAndSendKeys(By.xpath("//input[@ng-model='entity.named']"), form.getNamed()).
                // TODO enter Type
                //clearAndSendKeys(By.xpath("//label[text() = 'Type']/following::input[@type='text']"), form.getNamed()).
                submitButton(nextButton).
                submit();

        facade.form().
                completeComboBox(By.xpath("//label[text() = 'Archetype']/following::input[@type='text']"), form.getArchetypeFilter()).
                submitButton(nextButton).
                submit();
        untilNextWizardPage(facade, nextButton);

        facade.form().
                submitButton(nextButton).
                submit();
        untilNextWizardPage(facade, nextButton);

        facade.form().
                completeComboBox(By.xpath("//label[text() = 'Flow']/following::input[@type='text']"), form.getJenkinsFileFilter()).
                submitButton(nextButton).
                submit();

        // TODO how to assert it worked?
    }

    protected void untilNextWizardPage(WebDriverFacade facade, By nextButton) {
        facade.sleep(Millis.seconds(5));
        facade.untilIsEnabled(nextButton);
    }


    public By getCreateProjectBy() {
        return createProjectBy;
    }

    public By getSignInBy() {
        return signInBy;
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
