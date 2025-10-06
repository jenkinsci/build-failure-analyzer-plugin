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

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.BuildLogFailureReader;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import com.cloudbees.hudson.plugins.folder.Folder;
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
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the BuildLogIndication.
 */
@WithJenkins
class BuildLogIndicationFoldersTest {

    private static final String TEST_STRING = "teststring";

    private static final long WAIT_TIME_IN_SECONDS = 10;

    /**
     * Tests that the BuildLogFailureReader can find the string in the build log
     * on a one level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderSuccessfulNF1(JenkinsRule r) throws Exception {
        Folder f = createFolder(r);
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication(".*test.*");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNotNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader can find the string in the build log
     * on a two level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderSuccessfulNF2(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f = f0.createProject(Folder.class, "f2");
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication(".*test.*");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNotNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader can find the string in the build log
     * on a three level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderSuccessfulNF3(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f1 = f0.createProject(Folder.class, "f2");
        Folder f = f1.createProject(Folder.class, "f3");
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication(".*test.*");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNotNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader doesn't find the string in the build log
     * on a one level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderUnsuccessful(JenkinsRule r) throws Exception {
        Folder f = createFolder(r);
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication("correct horse battery staple");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader doesn't find the string in the build log
     * on a two level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderUnsuccessfulNF2(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f = f0.createProject(Folder.class, "f2");
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication("correct horse battery staple");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader doesn't find the string in the build log
     * on a three level folder.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testBuildLogFailureReaderUnsuccessfulNF3(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f1 = f0.createProject(Folder.class, "f2");
        Folder f = f1.createProject(Folder.class, "f3");
        FreeStyleProject project = f.createProject(FreeStyleProject.class, "foo");
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication("correct horse battery staple");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build);
        assertNull(found);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a freestyle build whose log contains a line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkFreestyleProject(JenkinsRule r) throws Exception {
        Folder f = createFolder(r);
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        String buildUrl = r.getURL() + freeStyleBuild.getUrl(); // buildUrl will end with /1/
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
     * to a freestyle build whose log contains a line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkFreestyleProjectNF2(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f = f0.createProject(Folder.class, "f2");
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        String buildUrl = r.getURL() + freeStyleBuild.getUrl(); // buildUrl will end with /1/
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
     * to a freestyle build whose log contains a line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkFreestyleProjectNF3(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f1 = f0.createProject(Folder.class, "f2");
        Folder f = f1.createProject(Folder.class, "f3");
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        String buildUrl = r.getURL() + freeStyleBuild.getUrl(); // buildUrl will end with /1/
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
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkMatrixProject(JenkinsRule r) throws Exception {
        Folder f = createFolder(r);
        MatrixProject matrixProject = f.createProject(MatrixProject.class, "foo");
        Axis axis1 = new Axis("Letter", "Alfa");
        Axis axis2 = new Axis("Number", "One", "Two");
        matrixProject.setAxes(new AxisList(axis1, axis2));
        matrixProject.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<MatrixBuild> future = matrixProject.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
        String buildUrl = r.getURL() + build.getUrl();
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
            buildUrl = r.getURL() + matrixRun.getUrl();
            formValidation = indicationDescriptor.doMatchText(".*Simulating.*", buildUrl, true);
            assertEquals("Simulating a specific result code FAILURE", formValidation.getMessage());
            assertEquals(FormValidation.Kind.OK, formValidation.kind);
        }
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a matrix build whose log contains a line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkMatrixProjectNF2(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f = f0.createProject(Folder.class, "f2");
        MatrixProject matrixProject = f.createProject(MatrixProject.class, "foo");
        Axis axis1 = new Axis("Letter", "Alfa");
        Axis axis2 = new Axis("Number", "One", "Two");
        matrixProject.setAxes(new AxisList(axis1, axis2));
        matrixProject.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<MatrixBuild> future = matrixProject.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
        String buildUrl = r.getURL() + build.getUrl();
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
            buildUrl = r.getURL() + matrixRun.getUrl();
            formValidation = indicationDescriptor.doMatchText(".*Simulating.*", buildUrl, true);
            assertEquals("Simulating a specific result code FAILURE", formValidation.getMessage());
            assertEquals(FormValidation.Kind.OK, formValidation.kind);
        }
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a matrix build whose log contains a line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidOkMatrixProjectNF3(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f1 = f0.createProject(Folder.class, "f2");
        Folder f = f1.createProject(Folder.class, "f3");
        MatrixProject matrixProject = f.createProject(MatrixProject.class, "foo");
        Axis axis1 = new Axis("Letter", "Alfa");
        Axis axis2 = new Axis("Number", "One", "Two");
        matrixProject.setAxes(new AxisList(axis1, axis2));
        matrixProject.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<MatrixBuild> future = matrixProject.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
        String buildUrl = r.getURL() + build.getUrl();
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
            buildUrl = r.getURL() + matrixRun.getUrl();
            formValidation = indicationDescriptor.doMatchText(".*Simulating.*", buildUrl, true);
            assertEquals("Simulating a specific result code FAILURE", formValidation.getMessage());
            assertEquals(FormValidation.Kind.OK, formValidation.kind);
        }
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log does not contain any line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidWarning(JenkinsRule r) throws Exception {
        Folder f = createFolder(r);
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = r.getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log does not contain any line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidWarningNF2(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f = f0.createProject(Folder.class, "f2");
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = r.getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log does not contain any line that matches the pattern.
     *
     * @param r
     *
     * @throws Exception if so.
     */
    @Test
    void testDoMatchTextUrlValidWarningNF3(JenkinsRule r) throws Exception {
        Folder f0 = createFolder(r);
        Folder f1 = f0.createProject(Folder.class, "f2");
        Folder f = f1.createProject(Folder.class, "f3");
        FreeStyleProject freeStyleProject = f.createProject(FreeStyleProject.class, "foo");
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = r.buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = r.getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    private static Folder createFolder(JenkinsRule r) throws IOException {
        return r.jenkins.createProject(Folder.class, "folder" + r.jenkins.getItems().size());
    }
}
