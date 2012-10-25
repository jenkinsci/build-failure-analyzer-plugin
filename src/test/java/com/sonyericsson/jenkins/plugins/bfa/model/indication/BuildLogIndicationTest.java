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

import com.sonyericsson.jenkins.plugins.bfa.model.BuildLogFailureReader;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
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
}
