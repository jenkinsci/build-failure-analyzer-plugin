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

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
import hudson.util.RunList;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jenkins.model.Jenkins;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.powermock.api.mockito.PowerMockito;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the ScanOnDemandBaseAction.
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 * @throws Exception if so.
 */
public class ScanOnDemandBaseActionTest extends HudsonTestCase {

    private static final String TO_PRINT = "ERROR";
    /**
     * Tests for performScanMethod by passing failed build.
     *
     * @throws Exception if so.
     */
    public void testPerformScanFailedProject() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        assertBuildStatus(Result.FAILURE, build);
        RunList sodbuilds = new RunList();
        sodbuilds.add(build);
        ScanOnDemandBaseAction sodbaseaction = new ScanOnDemandBaseAction(project);
        sodbaseaction.setBuildType("nonscanned");
        StaplerRequest mockrequest = PowerMockito.mock(StaplerRequest.class);
        StaplerResponse mockresponse = PowerMockito.mock(StaplerResponse.class);
        sodbaseaction.doPerformScan(mockrequest, mockresponse);
        ScanOnDemandQueue.shutdown();
        assertNotNull(build.getAction(FailureCauseBuildAction.class));

    }

    /**
     * Tests for performScanMethod by passing sucess build.
     *
     * @throws Exception if so.
     */
    public void testPerformScanSuccessProject() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        assertBuildStatus(Result.SUCCESS, build);
        RunList sodbuilds = new RunList();
        sodbuilds.add(build);
        ScanOnDemandBaseAction sodbaseaction = new ScanOnDemandBaseAction(project);
        sodbaseaction.setBuildType("nonscanned");
        StaplerRequest mockrequest = PowerMockito.mock(StaplerRequest.class);
        StaplerResponse mockresponse = PowerMockito.mock(StaplerResponse.class);
        sodbaseaction.doPerformScan(mockrequest, mockresponse);
        assertNull(build.getAction(FailureCauseBuildAction.class));
    }

    /**
     * Tests that the action is visible on the project page only when the user has the correct permissions.
     *
     * @throws Exception if problemos
     * @see ScanOnDemandBaseAction#hasPermission()
     */
    public void testShouldOnlyShowWhenHasPermission() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        String expectedHref = "/" + project.getUrl() + "scan-on-demand";

        SecurityRealm securityRealm = createDummySecurityRealm();
        Jenkins.getInstance().setSecurityRealm(securityRealm);
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.READ, "anonymous");
        strategy.add(Item.CONFIGURE, "bobby");
        strategy.add(Item.READ, "bobby");
        strategy.add(Jenkins.READ, "bobby");
        strategy.add(Item.READ, "alice");
        strategy.add(Jenkins.READ, "alice");
        Jenkins.getInstance().setAuthorizationStrategy(strategy);


        WebClient client = createWebClient();

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
    public void testShouldOnlyShowWhenScanningIsEnabled() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project = configRoundtrip(project);
        String expectedHref = "/" + project.getUrl() + "scan-on-demand";

        WebClient client = createWebClient();

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
