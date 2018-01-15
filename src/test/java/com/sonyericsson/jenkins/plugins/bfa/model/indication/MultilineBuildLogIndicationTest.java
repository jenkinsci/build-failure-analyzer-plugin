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

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.MultilineBuildLogFailureReader;
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
import hudson.model.Result;
import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.MockBuilder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Tests for the MultilineBuildLogIndication.
 */
public class MultilineBuildLogIndicationTest {

    /**
     * Jenkins Rule, cpt. Checkstyle Obvious
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRuleWithMatrixSupport j = new JenkinsRuleWithMatrixSupport();

    private static final String TEST_STRING = "teststring\nis here";
    private static final String FIRST_LINE_TEST_STRING = "teststring";

    private static final long WAIT_TIME_IN_SECONDS = 10;

    /**
     * Tests that the MultilineBuildLogFailureReader can find the string in the build log.
     * @throws Exception if so.
     */
    @Test
    public void testMultilineBuildLogFailureReaderSuccessful() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        MultilineBuildLogIndication indication = new MultilineBuildLogIndication("teststring.*here");
        MultilineBuildLogFailureReader reader = new MultilineBuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build, System.out);
        assertNotNull(found);
    }

    /**
     * Tests that the MultilineBuildLogFailureReader doesn't find the string in the build log.
     * @throws Exception if so.
     */
    @Test
    public void testMultilineBuildLogFailureReaderUnsuccessful() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        MultilineBuildLogIndication indication = new MultilineBuildLogIndication("correct horse battery staple");
        MultilineBuildLogFailureReader reader = new MultilineBuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build, System.out);
        assertNull(found);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string matches the pattern.
     */
    @Test
    public void testDoMatchTextPlainTextOk() {
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
              new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
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
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
                new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
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
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
                new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("teststring.*here", buildUrl, true);
        assertEquals(FIRST_LINE_TEST_STRING, formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("/1/", "/lastBuild");
        formValidation = indicationDescriptor.doMatchText("teststring.*here", buildUrl, true);
        assertEquals(FIRST_LINE_TEST_STRING, formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("lastBuild", "lastSuccessfulBuild");
        formValidation = indicationDescriptor.doMatchText("teststring.*here", buildUrl, true);
        assertEquals(FIRST_LINE_TEST_STRING, formValidation.getMessage());
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
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
                new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("/1/", "/lastFailedBuild");
        formValidation = indicationDescriptor.doMatchText("Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        buildUrl = buildUrl.replace("lastFailedBuild", "lastUnsuccessfulBuild");
        formValidation = indicationDescriptor.doMatchText("Started by.*", buildUrl, true);
        assertEquals("Started by user SYSTEM", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        List<MatrixRun> matrixRuns = build.getRuns();
        for (MatrixRun matrixRun : matrixRuns) {
            buildUrl = j.getURL() + matrixRun.getUrl();
            formValidation = indicationDescriptor.doMatchText("Simulating.*", buildUrl, true);
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
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0, new Cause.UserCause()).get();
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
                new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
        String buildUrl = j.getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid but the string is an invalid url,
     * i.e. a malformed url or a url which does not refer to any Jenkins build.
     */
    public void testDoMatchTextUrlInvalid() {
        MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor indicationDescriptor =
                new MultilineBuildLogIndication.MultilineBuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", "this_url_is_malformed", true);
        assertEquals(Messages.InvalidURL_Error(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        formValidation = indicationDescriptor.doMatchText("hi",
                "localhost/job/this_url_is_well_formed_but_does_not_refer_to_any_jenkins_job/1/", true);
        assertEquals(Messages.InvalidURL_Error(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }
}
