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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import static io.fabric8.systests.SeleniumTests.logError;
import static io.fabric8.systests.SeleniumTests.logWarn;

/**
 * Stores the {@link By} and value of an input field for a form
 */
public class InputValue {
    private final WebDriverFacade facade;
    private final By by;
    private final String value;

    public InputValue(WebDriverFacade facade, By by, String value) {
        this.facade = facade;
        this.by = by;
        this.value = value;
    }

    @Override
    public String toString() {
        return "InputValue{" +
                "by=" + by +
                ", value='" + value + '\'' +
                '}';
    }

    public By getBy() {
        return by;
    }

    public String getValue() {
        return value;
    }

    public WebDriverFacade getFacade() {
        return facade;
    }

    public WebElement doInput() {
        WebElement element = facade.findOptionalElement(by);
        if (element != null) {
            for (int i = 0; i < 10; i++) {
                try {
                    doInputOnElement(element);
                    return element;
                } catch (StaleElementReferenceException e) {
                    logWarn("Caught: " + e);
                    getFacade().sleep(Millis.seconds(5));
                }
            }
            logWarn("Failed to perform input on " + by + " to due repeated StaleElementReferenceException!");
        }
        return null;
    }

    protected void doInputOnElement(WebElement element) {
        element.clear();
        element.sendKeys(value);
    }
}
