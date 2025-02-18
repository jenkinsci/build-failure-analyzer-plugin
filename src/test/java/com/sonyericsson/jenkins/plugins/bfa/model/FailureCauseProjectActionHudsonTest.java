/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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
package com.sonyericsson.jenkins.plugins.bfa.model;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test Failure Cause project action.
 */
@WithJenkins
class FailureCauseProjectActionHudsonTest {

    /**
     * Should show failures of last completed build.
     *
     * @param j
     *
     * @throws Exception in some cases.
     */
    @Test
    void testShowLastFailureOnProjectPage(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        CommandInterpreter commandInterpreter;
        if (Functions.isWindows()) {
            commandInterpreter = new BatchFile("@if %BUILD_NUMBER% == 1 exit /b 1");
        } else {
            commandInterpreter = new Shell("test $BUILD_NUMBER -ne 1");
        }
        project.getBuildersList().add(commandInterpreter);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        FailureCauseProjectAction action = project.getAction(FailureCauseProjectAction.class);
        assertNotNull(action.getAction());
        assertEquals(build.getAction(FailureCauseBuildAction.class), action.getAction());

        j.buildAndAssertSuccess(project);
        assertNull(project.getAction(FailureCauseProjectAction.class).getAction());
    }
}
