/*
 * The MIT License
 *
 * Copyright 2016 Sony Mobile Communications Inc. All rights reserved.
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
package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import hudson.model.Result;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for WorkflowJobs.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 * @throws Exception if so.
 */
public class FailureCauseWorkFlowTest {
    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Tests that an action is added when the builds fail.
     *
     * @throws Exception if so.
     */
    @Test
    public void testWorkflowFailureCauseBuildAction() throws Exception {
        WorkflowJob proj = j.jenkins.createProject(WorkflowJob.class, "proj");
        proj.setDefinition(new CpsFlowDefinition("error()"));
        Future<WorkflowRun> f = proj.scheduleBuild2(0);
        assertThat("build was actually scheduled", f, Matchers.notNullValue());
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, f.get());
        FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
    }

    /**
     * Tests that no action is added if all builds are successful.
     *
     * @throws Exception if so.
     */
    @Test
    public void testFailureCausesWhenNotFailed() throws Exception {
        WorkflowJob proj = j.jenkins.createProject(WorkflowJob.class, "proj");
        proj.setDefinition(new CpsFlowDefinition("println('hello')"));
        WorkflowRun run = j.assertBuildStatusSuccess(proj.scheduleBuild2(0));
        FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }
}
