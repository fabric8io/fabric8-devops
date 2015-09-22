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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.fabric8.systests.SeleniumTests.logError;
import static io.fabric8.systests.SeleniumTests.logPageLocation;
import static io.fabric8.systests.SeleniumTests.logWarn;
import static org.junit.Assert.fail;


/**
 * A set of helper methods for writing {@link WebDriver} based tests
 */
public class WebDriverFacade {
    private static final transient Logger LOG = LoggerFactory.getLogger(WebDriverFacade.class);

    private final WebDriver driver;
    private long defaultTimeoutInSeconds = 10;

    public WebDriverFacade(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Finds an element or returns null if it could not be found
     */
    public WebElement findOptionalElement(By by) {
        try {
            return driver.findElement(by);
        } catch (NoSuchElementException e) {
            return null;
        } catch (Throwable e) {
            logError("Failed to find " + by, e);
            return null;
        }
    }

    /**
     * Finds the element for the `by`, clears the field and sends the given text
     *
     * @return the element or null if it is not found
     */
    public WebElement clearAndSendKeys(By by, String text) {
        WebElement field = findOptionalElement(by);
        if (field != null) {
            field.clear();
            field.sendKeys(text);
        }
        return field;
    }

    /**
     * Returns a form facade for submitting a form
     */
    public FormFacade form() {
        return new FormFacade(this);
    }

    public boolean until(ExpectedCondition<Boolean> condition) {
        return until(condition.toString(), defaultTimeoutInSeconds, condition);
    }

    public boolean until(String message, ExpectedCondition<Boolean> condition) {
        return until(message, defaultTimeoutInSeconds, condition);
    }

    public boolean until(String message, long timeoutInSeconds, ExpectedCondition<Boolean> condition) {
        return new WebDriverWait(driver, timeoutInSeconds).withMessage(message).until(condition);
    }

    public boolean untilLinkClicked(final By by) {
        return untilLinkClicked(defaultTimeoutInSeconds, by);
    }

    public boolean untilLinkClicked(long timeoutInSeconds, final By by) {
        String message = "click link " + by;
        return until(message, timeoutInSeconds, new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement link = findOptionalElement(by);
                if (link != null) {
                    System.out.println("Clicking link: " + by + " at " + driver.getCurrentUrl());
                    link.click();
                    System.out.println("Clicked link: " + by + " now at " + driver.getCurrentUrl());
                    return true;
                } else {
                    System.out.println("Not found link " + by + " at " + driver.getCurrentUrl());
                    return false;
                }
            }
        });
    }

    /**
     * Waits until one of the given elements is available
     */
    public void untilOneOf(final By... bys) {
        final List<By> byList = Arrays.asList(bys);
        String message = "One of these is available: " + byList;
        until(message, defaultTimeoutInSeconds, new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver driver) {
                logPageLocation(driver);
                for (By by : bys) {
                    WebElement element = findOptionalElement(by);
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        System.out.println("Found " + element + " for " + by + " at " + driver.getCurrentUrl());
                        return true;
                    }
                }
                System.out.println("Still not found any of " + byList + " at " + driver.getCurrentUrl());
                return false;
            }
        });
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public boolean currentUrlStartsWith(String expectedUrl) {
        String currentUrl = getDriver().getCurrentUrl();
        boolean answer = currentUrl != null && currentUrl.startsWith(expectedUrl);
        if (!answer) {
            logWarn("Current URL `" + currentUrl + "` does not start with `" + expectedUrl + "`");
        }
        return answer;
    }

    public void assertCurrentUrlStartsWith(String expectedUrl) {
        String currentUrl = getDriver().getCurrentUrl();
        boolean answer = currentUrl != null && currentUrl.startsWith(expectedUrl);
        if (!answer) {
            fail("Current URL `" + currentUrl + "` does not start with `" + expectedUrl + "`");
        }
    }

    /**
     * Lets wait until the link is visible then click it. If the click doesn't work lets retry a few times
     * just in case
     */
    public void untilLinkClickedLoop(By by, String expectedUrl) {
        for (int i = 0; i < 5; i++) {
            untilLinkClicked(by);
            sleep(Millis.seconds(10));

            if (currentUrlStartsWith(expectedUrl)) {
                break;
            } else {
                logWarn("lets try re-clicking link: " + by);
            }
        }
        assertCurrentUrlStartsWith(expectedUrl);
    }

}
