/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.login.feature;

import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class LoginIT {

    @Autowired
    WebDriver webDriver;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Test
    public void testLoggingIn() throws Exception {
        webDriver.get(baseUrl + "/login");
        Assert.assertEquals("Pivotal", webDriver.getTitle());

        webDriver.findElement(By.name("username")).sendKeys("marissa");
        webDriver.findElement(By.name("password")).sendKeys("koala");
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();

        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), Matchers.containsString("Where to?"));
    }

    @Test
    public void testGoogleAnalytics() throws Exception {
        webDriver.get(baseUrl + "/login");
        if (webDriver instanceof JavascriptExecutor) {
            Assert.assertNotNull(((JavascriptExecutor) webDriver).executeScript("return window.ga;"));
        } else {
            Assert.fail("expected a JavascriptExecutor WebDriver");
        }
    }

    @Test
    public void testBuildInfo() throws Exception {
        webDriver.get(baseUrl + "/login");

        String regex = "Version: \\S+, Commit: \\w{7}, Timestamp: \\S+, UAA: http://localhost:8080/uaa";
        Assert.assertTrue(webDriver.findElement(By.className("footer")).getAttribute("title").matches(regex));
    }

    @Test
    public void testSignupPage() throws Exception {
        webDriver.get(baseUrl + "/login");

        webDriver.findElement(By.linkText("Create account")).click();

        Assert.assertEquals("https://network.gopivotal.com/registrations/new", webDriver.findElement(By.linkText("Pivotal Network")).getAttribute("href"));
        Assert.assertEquals("https://console.10.244.0.34.xip.io/register", webDriver.findElement(By.linkText("Pivotal Web Services")).getAttribute("href"));
    }
}
