/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.MatrixSupport;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the FailureCauseMatrixAggregator.
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@WithJenkins
class FailureCauseMatrixAggregatorTest {

    /**
     * Tests that an action is added when the builds fail.
     * Also tests getRunsWithAction.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testAggregateFailureCauses(JenkinsRule jenkins) throws Exception {
        MatrixProject matrix = MatrixSupport.createMatrixProject(jenkins);
        Axis axis = new Axis("Axel", "Foley", "Rose");
        matrix.setAxes(new AxisList(axis));
        matrix.getBuildersList().add(new MockBuilder(Result.FAILURE));
        QueueTaskFuture<MatrixBuild> future = matrix.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseMatrixBuildAction matrixAction = build.getAction(FailureCauseMatrixBuildAction.class);
        assertNotNull(matrixAction);
        assertThat(matrixAction.getRunsWithAction().size(), is(2));
    }

    /**
     * Tests that no action is added if all builds are successful.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testAggregateFailureCausesWhenNotFailed(JenkinsRule jenkins) throws Exception {
        MatrixProject matrix = MatrixSupport.createMatrixProject(jenkins);
        Axis axis = new Axis("Axel", "Foley", "Rose");
        matrix.setAxes(new AxisList(axis));
        QueueTaskFuture<MatrixBuild> future = matrix.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(10, TimeUnit.SECONDS);
        Action matrixAction = build.getAction(FailureCauseMatrixBuildAction.class);
        assertNull(matrixAction);
    }

    /**
     * Tests that an action is not added when the builds abort.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testAggregateIgnoreAbortedCauses(JenkinsRule jenkins) throws Exception {
        MatrixProject matrix = MatrixSupport.createMatrixProject(jenkins);
        PluginImpl.getInstance().setDoNotAnalyzeAbortedJob(true);
        Axis axis = new Axis("Axel", "Foley", "Rose");
        matrix.setAxes(new AxisList(axis));
        matrix.getBuildersList().add(new MockBuilder(Result.ABORTED));
        QueueTaskFuture<MatrixBuild> future = matrix.scheduleBuild2(0, new Cause.UserIdCause());
        MatrixBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseMatrixBuildAction matrixAction = build.getAction(FailureCauseMatrixBuildAction.class);
        assertNull(matrixAction);
    }
}
