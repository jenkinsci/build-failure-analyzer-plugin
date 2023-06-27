/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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
package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.util.UrlUtils;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.BuildLogFailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.JenkinsRuleWithMatrixSupport;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for the BuildLogIndication.
 */
public class BuildLogIndicationTest {

    private static final String TEST_STRING = "teststring";

    private static final long WAIT_TIME_IN_SECONDS = 10;

    /**
     * The test harness.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRuleWithMatrixSupport j = new JenkinsRuleWithMatrixSupport();

    /**
     * Tests that the BuildLogFailureReader can find the string in the build log.
     * @throws Exception if so.
     */
    @Test
    public void testBuildLogFailureReaderSuccessful() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication(".*test.*");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNotNull(found);
    }

    /**
     * Tests that the FailureReader parses two difference indications of same cause and description in result.
     * @throws Exception if so.
     */
    @Test
    public void testFailureReaderForTwoIndications() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder("bla Build timed out bla"));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        FailureCause cause = new FailureCause("bla", "${1,1}", "comment");

        cause.addIndication(new BuildLogIndication("(.*timed out.*)"));
        cause.addIndication(new BuildLogIndication("(.*Build timed out.*)"));

        List<FailureCause> causes = new ArrayList<FailureCause>();
        causes.add(cause);

        List<FoundFailureCause> foundFailureCause = FailureReader.scanSingleLinePatterns(causes, build,
                new BufferedReader(build.getLogReader()), "test");
        assertEquals(foundFailureCause.size(), 1);
        assertEquals(foundFailureCause.get(0).getIndications().size(), 2);
        assertEquals(foundFailureCause.get(0).getDescription(), "bla Build timed out bla");
    }

    /**
     * Tests that Failure reader stops to check indication after first occurrence.
     * @throws Exception if so.
     */
    @Test
    public void testFailureReaderStopsOnFirstOccurrence() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder("bla Build timed out bla\nbla Build timed out bla"));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        FailureCause cause = new FailureCause("bla", "${1,1}", "comment");
        cause.addIndication(new BuildLogIndication("(.*timed out.*)"));

        List<FailureCause> causes = new ArrayList<FailureCause>();
        causes.add(cause);

        List<FoundFailureCause> foundFailureCause = FailureReader.scanSingleLinePatterns(causes, build,
                new BufferedReader(build.getLogReader()), "test");
        assertEquals(foundFailureCause.size(), 1);
        assertEquals(foundFailureCause.get(0).getIndications().size(), 1);
        assertEquals(foundFailureCause.get(0).getDescription(), "bla Build timed out bla");
    }

    /**
     * Tests that the BuildLogFailureReader doesn't find the string in the build log.
     * @throws Exception if so.
     */
    @Test
    public void testBuildLogFailureReaderUnsuccessful() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication("correct horse battery staple");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNull(found);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string matches the pattern.
     */
    @Test
    public void testDoMatchTextPlainTextOk() {
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
              new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText(".*", "hello", false);
        assertEquals(Messages.StringMatchesPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string does not
     * match the pattern.
     */
    @Test
    public void testDoMatchTextPlainTextWarning() {
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", "hello", false);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a freestyle build whose log contains a line that matches the pattern.
     * @throws Exception if so.
     */
    @Test
    public void testDoMatchTextUrlValidOkFreestyleProject() throws Exception {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = j.buildAndAssertSuccess(freeStyleProject);
        String buildUrl = j.getURL() + freeStyleBuild.getUrl(); // buildUrl will end with /1/
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText(".*test\\D+", buildUrl, true);
        assertEquals(TEST_STRING, formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("/1/", "/lastBuild");
        formValidation = indicationDescriptor.doMatchText(".*test\\D+", buildUrl, true);
        assertEquals(TEST_STRING, formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("lastBuild", "lastSuccessfulBuild");
        formValidation = indicationDescriptor.doMatchText(".*test\\D+", buildUrl, true);
        assertEquals(TEST_STRING, formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a matrix build whose log contains a line that matches the pattern.
     * @throws Exception if so.
     */
    @Test
    public void testDoMatchTextUrlValidOkMatrixProject() throws Exception {
        MatrixProject matrixProject = j.createMatrixProject();
        Axis axis1 = new Axis("Letter", "Alfa");
        Axis axis2 = new Axis("Number", "One", "Two");
        matrixProject.setAxes(new AxisList(axis1, axis2));
        matrixProject.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<MatrixBuild> future = matrixProject.scheduleBuild2(0, new Cause.UserCause());
        MatrixBuild build = future.get(WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
        String buildUrl = j.getURL() + build.getUrl();
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText(".*Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("/1/", "/lastFailedBuild");
        formValidation = indicationDescriptor.doMatchText(".*Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("lastFailedBuild", "lastUnsuccessfulBuild");
        formValidation = indicationDescriptor.doMatchText(".*Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        List<MatrixRun> matrixRuns = build.getRuns();
        for (MatrixRun matrixRun : matrixRuns) {
            buildUrl = j.getURL() + matrixRun.getUrl();
            formValidation = indicationDescriptor.doMatchText(".*Simulating.*", buildUrl, true);
            assertEquals("Simulating a specific result code FAILURE", formValidation.getMessage());
            assertEquals(FormValidation.Kind.OK, formValidation.kind);
        }
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log does not contain any line that matches the pattern.
     * @throws Exception if so.
     */
    @Test
    public void testDoMatchTextUrlValidWarning() throws Exception {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = j.buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = j.getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid but the string is an invalid url,
     * i.e. a malformed url or a url which does not refer to any Jenkins build.
     */
    @Test
    public void testDoMatchTextUrlInvalid() {
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", "this_url_is_malformed", true);
        assertEquals(Messages.InvalidURL_Error(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        formValidation = indicationDescriptor.doMatchText("hi",
                "localhost/job/this_url_is_well_formed_but_does_not_refer_to_any_jenkins_job/1/", true);
        assertEquals(Messages.InvalidURL_Error(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    // CS IGNORE MagicNumber FOR NEXT 200 LINES. REASON: test data.

    /**
     * Tests SECURITY-1651.
     * @throws Exception if so
     */
    @Test @Issue("SECURITY-1651")
    public void testDoMatchNotHttpGetAccessible() throws Exception {
        lockDown();
        final JenkinsRule.WebClient webClient = j.createWebClient();
        webClient.assertFails("descriptorByName/"
                + BuildLogIndication.class.getName()
                + "/matchText?"
                + "pattern=[a-z]+&"
                + "testText=hello&"
                + "textSourceIsUrl=false",
                405);
    }

    /**
     * Tests SECURITY-1651.
     * @throws Exception if so
     */
    @Test @Issue("SECURITY-1651")
    public void testDoMatchHttpPostAccessible() throws Exception {
        lockDown();
        final JenkinsRule.WebClient webClient = j.createWebClient();
        post(webClient, "descriptorByName/"
                        + BuildLogIndication.class.getName()
                        + "/matchText?"
                        + "pattern=[a-z]+&"
                        + "testText=hello&"
                        + "textSourceIsUrl=false",
                null, 403);
    }

    /**
     * Tests SECURITY-1651.
     * @throws Exception if so
     */
    @Test @Issue("SECURITY-1651")
    public void testDoMatchHttpPostAccessibleWithPermission() throws Exception {
        lockDown();
        final JenkinsRule.WebClient webClient = j.createWebClient().login("bob");
        post(webClient, "descriptorByName/"
                        + BuildLogIndication.class.getName()
                        + "/matchText?"
                        + "pattern=[a-z]+&"
                        + "testText=hello&"
                        + "textSourceIsUrl=false",
                null, null);
    }

    /**
     * Performs an HTTP POST request to the relative url.
     *
     * @param webClient the client
     * @param relative the url relative to the context path
     * @param expectedContentType if expecting specific content type or null if not
     * @param expectedStatus if expecting a failing http status code or null if not
     * @throws IOException if so
     */
    private static void post(JenkinsRule.WebClient webClient, String relative,
                             String expectedContentType, Integer expectedStatus) throws IOException {
        WebRequest request = new WebRequest(
                UrlUtils.toUrlUnsafe(webClient.getContextPath() + relative),
                HttpMethod.POST);
        try {
            Page p = webClient.getPage(request);
            if (expectedContentType != null) {
                assertThat(p.getWebResponse().getContentType(), is(expectedContentType));
            }
        } catch (FailingHttpStatusCodeException e) {
            if (expectedStatus != null) {
                assertEquals(expectedStatus.intValue(), e.getStatusCode());
            } else {
                throw e;
            }
        }
    }

    /**
     * Lock down the instance.
     */
    private void lockDown() {
        SecurityRealm securityRealm = j.createDummySecurityRealm();
        j.getInstance().setSecurityRealm(securityRealm);
        j.getInstance().setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().toAuthenticated());
        j.jenkins.setCrumbIssuer(null); //Not really testing csrf right now
        j.getInstance().setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Item.READ, Item.DISCOVER).everywhere().toAuthenticated()
                        .grant(PluginImpl.VIEW_PERMISSION).everywhere().toAuthenticated()
                        .grant(Jenkins.READ, Item.DISCOVER).everywhere().toEveryone()
                        .grant(Item.CONFIGURE).everywhere().to("bob")
                        .grant(PluginImpl.UPDATE_PERMISSION).everywhere().to("bob")
                        .grant(Jenkins.ADMINISTER).everywhere().to("alice"));
    }
}
