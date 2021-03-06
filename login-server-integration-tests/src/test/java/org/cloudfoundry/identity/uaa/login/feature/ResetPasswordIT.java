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

import static org.hamcrest.Matchers.containsString;

import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class ResetPasswordIT {

    @Autowired
    WebDriver webDriver;

    @Autowired
    SimpleSmtpServer simpleSmtpServer;

    @Autowired
    TestClient testClient;

    @Autowired
    RestTemplate restTemplate;

    @Value("${integration.test.base_url}")
    String baseUrl;

    private String userEmail;
    private String userName;

    @Before
    public void setUp() throws Exception {
        int randomInt = new SecureRandom().nextInt();

        String adminAccessToken = testClient.getOAuthAccessToken("admin", "adminsecret", "client_credentials", "clients.read clients.write clients.secret");
        String scimClientId = "scim" + randomInt;
        testClient.createScimClient(adminAccessToken, scimClientId);
        String scimAccessToken = testClient.getOAuthAccessToken(scimClientId, "scimsecret", "client_credentials", "scim.read scim.write password.write");
        userEmail = "user" + randomInt + "@example.com";
        userName = "JOE" + randomInt;
        testClient.createUser(scimAccessToken, userName, userEmail, "secret");
    }

    @Test
    public void resettingAPassword() throws Exception {
        webDriver.get(baseUrl + "/login");
        Assert.assertEquals("Pivotal", webDriver.getTitle());

        webDriver.findElement(By.linkText("Reset password")).click();

        webDriver.findElement(By.name("email")).sendKeys(userEmail);
        webDriver.findElement(By.xpath("//input[@value='Send reset password link']")).click();

        Assert.assertEquals(1, simpleSmtpServer.getReceivedEmailSize());
        SmtpMessage message = (SmtpMessage) simpleSmtpServer.getReceivedEmail().next();
        Assert.assertEquals(userEmail, message.getHeaderValue("To"));
        Assert.assertThat(message.getBody(), containsString("Click the link to reset your password"));

        Assert.assertEquals("Check your email for a reset password link.", webDriver.findElement(By.cssSelector(".instructions-sent")).getText());

        String link = extractLink(message.getBody());
        webDriver.get(link);

        webDriver.findElement(By.name("password")).sendKeys("newsecret");
        webDriver.findElement(By.name("password_confirmation")).sendKeys("newsecret");

        webDriver.findElement(By.xpath("//input[@value='Create new password']")).click();

        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));

        webDriver.findElement(By.xpath("//*[text()='"+userName+"']")).click();
        webDriver.findElement(By.linkText("Sign Out")).click();

        webDriver.findElement(By.name("username")).sendKeys(userName);
        webDriver.findElement(By.name("password")).sendKeys("newsecret");
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();

        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
    }

    private String extractLink(String messageBody) {
        Pattern linkPattern = Pattern.compile("<a href=\"(.*?)\">.*?</a>");
        Matcher matcher = linkPattern.matcher(messageBody);
        matcher.find();
        return matcher.group(1);
    }
}
