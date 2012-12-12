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
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for the BuildLogIndication.
 */
public class BuildLogIndicationTest extends HudsonTestCase {

    private static final String TEST_STRING = "teststring";

    /**
     * Tests that the BuildLogFailureReader can find the string in the build log.
     * @throws Exception if so.
     */
    public void testBuildLogFailureReaderSuccessful() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication(".*test.*");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build, System.out);
        assertNotNull(found);
    }

    /**
     * Tests that the BuildLogFailureReader doesn't find the string in the build log.
     * @throws Exception if so.
     */
    public void testBuildLogFailureReaderUnsuccessful() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild build = buildAndAssertSuccess(project);
        BuildLogIndication indication = new BuildLogIndication("correct horse battery staple");
        BuildLogFailureReader reader = new BuildLogFailureReader(indication);
        FoundIndication found = reader.scan(build, System.out);
        assertNull(found);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string matches the pattern.
     */
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
    public void testDoMatchTextPlainTextWarning() {
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", "hello", false);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log contains a line that matches the pattern.
     * @throws Exception if so.
     */
    public void testDoMatchTextUrlValidOk() throws Exception {
        FreeStyleProject freeStyleProject = createFreeStyleProject();
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = getURL() + freeStyleBuild.getUrl(); // buildUrl will end with /1/
        FormValidation formValidation = indicationDescriptor.doMatchText(".*test.*", buildUrl, true);
        assertEquals("teststring", formValidation.getMessage());
        assertEquals(FormValidation.Kind.OK, formValidation.kind);

        // TODO Test that doMatchText correctly handles builds whose url ends with lastBuild,
        // lastSuccessfulBuild etc., and that doMatchText correctly handles matrix builds
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid and the string is a url
     * to a build whose log does not contain any line that matches the pattern.
     * @throws Exception if so.
     */
    public void testDoMatchTextUrlValidWarning() throws Exception {
        FreeStyleProject freeStyleProject = createFreeStyleProject();
        freeStyleProject.getBuildersList().add(new PrintToLogBuilder(TEST_STRING));
        FreeStyleBuild freeStyleBuild = buildAndAssertSuccess(freeStyleProject);
        BuildLogIndication.BuildLogIndicationDescriptor indicationDescriptor =
                new BuildLogIndication.BuildLogIndicationDescriptor();
        String buildUrl = getURL() + freeStyleBuild.getUrl();
        FormValidation formValidation = indicationDescriptor.doMatchText("hi", buildUrl, true);
        assertEquals(Messages.StringDoesNotMatchPattern(), formValidation.getMessage());
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    /**
     * Tests that the doMatchText method behaves correctly when the pattern is valid but the string is an invalid url,
     * i.e. a malformed url or a url which does not refer to any Jenkins build.
     */
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
}
