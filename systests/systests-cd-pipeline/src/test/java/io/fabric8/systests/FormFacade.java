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
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * A helper facade for submitting forms
 */
public class FormFacade extends PageSupport {
    private List<InputValue> inputValues = new ArrayList<>();
    private By submitBy;

    public FormFacade(WebDriverFacade facade) {
        super(facade);
    }

    public FormFacade clearAndSendKeys(By by, String value) {
        inputValues.add(new InputValue(getFacade(), by, value));
        return this;
    }


    public FormFacade submitButton(By submitBy) {
        this.submitBy = submitBy;
        return this;
    }

    public void submit() {
        getFacade().until("Form inputs: " + inputValues, new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                System.out.println("Now waiting for the: " + inputValues + " on " + driver.getCurrentUrl());
                WebElement submitElement = null;
                for (InputValue inputValue : inputValues) {
                    submitElement = inputValue.clearAndSendKeys();
                    if (submitElement == null) {
                        System.out.println("Missing " + inputValue + " at " + driver.getCurrentUrl());
                        return false;
                    }
                }
                if (submitBy == null && submitElement == null) {
                    fail("No input fields submitted yet at " + driver.getCurrentUrl());
                    return false;
                } else {
                    getFacade().sleep(Millis.seconds(5));
                    if (submitBy != null) {
                        submitElement = getFacade().findOptionalElement(submitBy);
                        if (submitElement == null) {
                            fail("Could not find submit button " + submitBy + " at " + driver.getCurrentUrl());
                            return false;
                        } else {
                            System.out.println("Submitting form: " + inputValues + " on " + submitElement + " at " + driver.getCurrentUrl());
                            submitElement.click();
                        }
                    } else {
                        System.out.println("Submitting form: " + inputValues + " on " + submitElement + " at " + driver.getCurrentUrl());
                        submitElement.submit();
                    }
                    return true;
                }
            }
        });
    }
}
