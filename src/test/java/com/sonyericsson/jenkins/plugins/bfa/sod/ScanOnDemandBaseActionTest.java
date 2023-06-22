/*
 * The MIT License
 *
 * Copyright 2014 Sony Mobile Communications AB. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.bfa.sod;

import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.SecurityRealm;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for {@link ScanOnDemandBaseAction}.
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandBaseActionTest {

    //CS IGNORE VisibilityModifier FOR NEXT 5 LINES. REASON: by design
    /**
     * The Jenkins rule, duh.
     */
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String TO_PRINT = "ERROR";
    /**
     * Tests for performScanMethod by passing failed build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testPerformScanFailedProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        j.assertBuildStatus(Result.FAILURE, build);
        j.createWebClient().getPage(project, "scan-on-demand/nonscanned/performScan");
        ScanOnDemandQueue.shutdown();
        assertNotNull(build.getAction(FailureCauseBuildAction.class));

    }

    /**
     * Tests for performScanMethod by passing sucess build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testPerformScanSuccessProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        j.assertBuildStatus(Result.SUCCESS, build);
        j.createWebClient().getPage(project, "scan-on-demand/nonscanned/performScan");
        ScanOnDemandQueue.shutdown();
        assertNull(build.getAction(FailureCauseBuildAction.class));
    }

    /**
     * Tests that the action is visible on the project page only when the user has the correct permissions.
     *
     * @throws Exception if problemos
     * @see ScanOnDemandBaseAction#hasPermission()
     */
    @Test
    public void testShouldOnlyShowWhenHasPermission() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        String expectedHref = "/jenkins/" + project.getUrl() + "scan-on-demand";

        SecurityRealm securityRealm = j.createDummySecurityRealm();
        Jenkins.getInstance().setSecurityRealm(securityRealm);
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.READ, "anonymous");
        strategy.add(Item.CONFIGURE, "bobby");
        strategy.add(Item.READ, "bobby");
        strategy.add(Jenkins.READ, "bobby");
        strategy.add(Item.READ, "alice");
        strategy.add(Jenkins.READ, "alice");
        Jenkins.getInstance().setAuthorizationStrategy(strategy);


        JenkinsRule.WebClient client = j.createWebClient();

        HtmlPage page = client.login("alice").getPage(project);
        try {
            HtmlAnchor anchor = page.getAnchorByHref(expectedHref);
            fail("Alice can see the link!");
        } catch (ElementNotFoundException e) {
            System.out.println("Didn't find the link == good!");
        }

        //then test the opposite

        client = client.login("bobby");
        page = client.getPage(project);
        HtmlAnchor anchor = page.getAnchorByHref(expectedHref);
        assertNotNull(anchor);
    }

    /**
     * Tests that the action is visible on the project page only when scanning is enabled.
     *
     * @throws Exception if problemos
     */
    @Test
    public void testShouldOnlyShowWhenScanningIsEnabled() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project = (FreeStyleProject)j.configRoundtrip((Item)project);
        String expectedHref = "/jenkins/" + project.getUrl() + "scan-on-demand";

        JenkinsRule.WebClient client = j.createWebClient();

        //Assert visible
        HtmlPage page = client.getPage(project);
        HtmlAnchor anchor = page.getAnchorByHref(expectedHref);
        assertNotNull(anchor);

        //Assert gone when disabled
        project.removeProperty(ScannerJobProperty.class);
        project.addProperty(new ScannerJobProperty(true));
        //Just check it in case...
        assertFalse(PluginImpl.shouldScan(project));

        page = client.getPage(project);
        try {
            anchor = page.getAnchorByHref(expectedHref);
            fail("We can see the link!");
        } catch (ElementNotFoundException e) {
            System.out.println("Didn't find the link == good!");
        }
    }
 }
