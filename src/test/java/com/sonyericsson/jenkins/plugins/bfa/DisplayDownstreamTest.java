package com.sonyericsson.jenkins.plugins.bfa;
/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications Inc. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.MatrixSupport;
import hudson.Functions;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//CS IGNORE MagicNumber FOR NEXT 260 LINES. REASON: TestData

/**
 * Test fetching display data object for build failure analysis of downstream
 * objects.
 *
 * @author Jan-Olof Sivtoft
 */
@WithJenkins
class DisplayDownstreamTest {

    private static final String MATRIX_PROJECT_NEWS = "NEWS";
    private static final String PROJECT_NE = "NORTH-EAST";
    private static final String PROJECT_EW = "NORTH-WEST";
    private static final String PROJECT_SE = "SOUTH-EAST";
    private static final String PROJECT_SW = "SOUTH-WEST";
    private static final String DEFAULT;

    static {
        if (Functions.isWindows()) {
            DEFAULT = "echo I am %PROJECT_NAME%";
        } else {
            DEFAULT = "echo I am ${PROJECT_NAME}";
        }
    }

    private static final String FAILED = "rapakalja";

    /**
     * Test the FailureCauseDisplayData object.
     *
     * @param jenkins
     *
     * @throws Exception if failure  build can't be executed
     */
    @Test
    void testFailureCauseDisplayData(JenkinsRule jenkins) throws Exception {
        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild(jenkins));

        Assertions.assertNotNull(failureCauseDisplayData.getDownstreamFailureCauses());
        Assertions.assertNotNull(failureCauseDisplayData.getFoundFailureCauses());

        FailureCauseDisplayData.Links links =
                failureCauseDisplayData.getLinks();
        Assertions.assertNotNull(links);

        assertEquals("WEST,SOUTH", links.getProjectDisplayName());
        Assertions.assertNotNull(links.getProjectUrl());
        assertEquals("#1", links.getBuildDisplayName());
        Assertions.assertNotNull(links.getBuildUrl());
    }


    /**
     * Test FailureCauseDisplayData object population when no indication is
     * specified.
     *
     * @param jenkins
     *
     * @throws Exception if failure build can't be executed
     */
    @Test
    void testMatrixNoIdentifiedCause(JenkinsRule jenkins) throws Exception {
        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild(jenkins));

        // It is not the matrix run that fails
        Assertions.assertTrue(failureCauseDisplayData.getFoundFailureCauses().isEmpty());
        assertEquals(1, failureCauseDisplayData.
                getDownstreamFailureCauses().size());

        FailureCauseDisplayData downstreamFailureCauseDisplayData =
                failureCauseDisplayData.getDownstreamFailureCauses().
                        get(0);

        List<FoundFailureCause> causeListFromAction =
                downstreamFailureCauseDisplayData.getFoundFailureCauses();

        // No indication added so this is expected
        Assertions.assertTrue(causeListFromAction.isEmpty());

        assertEquals(PROJECT_SW, downstreamFailureCauseDisplayData.
                getLinks().getProjectDisplayName());
    }

    /**
     * Test FailureCauseDisplayData object population when an indication is
     * specified.
     *
     * @param jenkins
     *
     * @throws Exception if failure cause cant be configured or build can't
     *                   be executed
     */
    @Test
    void testMatrixIdentifiedCause(JenkinsRule jenkins) throws Exception {
        Indication indication = new BuildLogIndication(".*" + FAILED + ".*");
        FailureCause failureCause = BuildFailureScannerHudsonTest.
                configureCauseAndIndication("Other cause", "Other description", "Other comment",
                        "Category", indication);

        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild(jenkins));

        // It is not the matrix run that fails
        Assertions.assertTrue(failureCauseDisplayData.getFoundFailureCauses().isEmpty());
        assertEquals(1, failureCauseDisplayData.
                getDownstreamFailureCauses().size());

        FailureCauseDisplayData downstreamFailureCauseDisplayData =
                failureCauseDisplayData.getDownstreamFailureCauses().
                        get(0);

        List<FoundFailureCause> causeListFromAction =
                downstreamFailureCauseDisplayData.getFoundFailureCauses();

        assertEquals(1, causeListFromAction.size());

        // This is the expected indication
        Assertions.assertTrue(BuildFailureScannerHudsonTest.findCauseInList(
                causeListFromAction, failureCause));

        assertEquals(PROJECT_SW, downstreamFailureCauseDisplayData.getLinks().
                getProjectDisplayName());
    }

    /**
     * Test FailureCauseDisplayData object population when an indication is
     * specified.
     *
     * @param jenkins
     *
     * @throws Exception if failure cause cant be configured or build can't
     *                   be executed
     */
    @Test
    void testIdentifiedTwoCauses(JenkinsRule jenkins) throws Exception {
        final FreeStyleProject child1 = createFreestyleProjectWithShell(jenkins, "child1", FAILED);
        final FreeStyleProject child2 = createFreestyleProjectWithShell(jenkins, "child2", FAILED);

        final FreeStyleProject parent = jenkins.createFreeStyleProject("parent");
        parent.getBuildersList().add(new TriggerBuilder(
                new BlockableBuildTriggerConfig(child1.getName() + ", " + child2.getName(),
                    new BlockingBehaviour(Result.FAILURE, Result.FAILURE, Result.FAILURE),
                        new ArrayList<>())));

        final Indication indication = new BuildLogIndication(".*" + FAILED + ".*");
        BuildFailureScannerHudsonTest.configureCauseAndIndication("Other cause",
                "Other description", "Other comment", "Category", indication);

        parent.scheduleBuild2(0).get();

        final FailureCauseBuildAction buildAction = parent.getFirstBuild().getAction(FailureCauseBuildAction.class);
        final FailureCauseDisplayData failureCauseDisplayData = buildAction.getFailureCauseDisplayData();
        final List<FailureCauseDisplayData> downstreamCauses = failureCauseDisplayData.getDownstreamFailureCauses();

        assertEquals(0, failureCauseDisplayData.getFoundFailureCauses().size());
        assertEquals(2, downstreamCauses.size());

        for (FailureCauseDisplayData causeDisplayData : downstreamCauses) {
            final List<FoundFailureCause> causeListFromAction = causeDisplayData.getFoundFailureCauses();
            assertEquals(1, causeListFromAction.size());
            assertEquals("Other cause", causeListFromAction.get(0).getName());
        }
    }

    /**
     * Test FailureCauseDisplayData object population from pipeline (using build-cache-dbf).
     *
     * @param jenkins
     *
     * @throws Exception if failure cause cant be configured or build can't
     *                   be executed
     */
    @Test
    void testIdentifiedTwoCausesFromPipeline(JenkinsRule jenkins) throws Exception {
        final FreeStyleProject child1 = createFreestyleProjectWithShell(jenkins, "child1", FAILED);
        final FreeStyleProject child2 = createFreestyleProjectWithShell(jenkins, "child2", FAILED);

        final WorkflowJob parent = jenkins.createProject(WorkflowJob.class, "parent");
        parent.setDefinition(new CpsFlowDefinition("try {build('child1')} catch (e) {};build('child2')", true));

        final Indication indication = new BuildLogIndication(".*" + FAILED + ".*");
        BuildFailureScannerHudsonTest.configureCauseAndIndication("Other cause",
                "Other description", "Other comment", "Category", indication);

        parent.scheduleBuild2(0).get();

        final FailureCauseBuildAction buildAction = parent.getFirstBuild().getAction(FailureCauseBuildAction.class);
        final FailureCauseDisplayData failureCauseDisplayData = buildAction.getFailureCauseDisplayData();
        final List<FailureCauseDisplayData> downstreamCauses = failureCauseDisplayData.getDownstreamFailureCauses();

        assertEquals(0, failureCauseDisplayData.getFoundFailureCauses().size());
        assertEquals(2, downstreamCauses.size());

        for (FailureCauseDisplayData causeDisplayData : downstreamCauses) {
            final List<FoundFailureCause> causeListFromAction = causeDisplayData.getFoundFailureCauses();
            assertEquals(1, causeListFromAction.size());
            assertEquals("Other cause", causeListFromAction.get(0).getName());
        }
    }

    /**
     * Creates and executes a matrix build.
     *
     * @param jenkins
     *
     * @return a Matrix build object
     * @throws Exception if build couldn't be executed
     */
    private static MatrixBuild executeBuild(JenkinsRule jenkins) throws Exception {

        MatrixProject matrixProject = createMatrixProjectNews(jenkins);

        matrixProject.setQuietPeriod(0);
        jenkins.getInstance().rebuildDependencyGraph();
        matrixProject.scheduleBuild2(0, new Cause.UserIdCause()).get();

        return matrixProject.getLastBuild();
    }

    /**
     * Returns FailureCauseDisplayData form the first MatrixBuild run
     *
     * @param build the executed build
     * @return a FailureCauseDisplayData object
     */
    private static FailureCauseDisplayData getDisplayData(MatrixBuild build) {

        FailureCauseMatrixBuildAction action =
                build.getAction(FailureCauseMatrixBuildAction.class);

        // This should be the PROJECT_SW
        MatrixRun runProjectSW = action.getRunsWithAction().get(0);

        return FailureCauseMatrixBuildAction.getFailureCauseDisplayData(runProjectSW);
    }

    /**
     * Creates a Matrix project with two axis. Four Downstream jobs are also
     * created. One of these will fail.
     *
     * @param jenkins
     *
     * @return MatrixProject
     *
     * @throws Exception if project(s) can't be created
     */
    private static MatrixProject createMatrixProjectNews(JenkinsRule jenkins) throws Exception {

        createFreestyleProjectWithDefaultShell(
                jenkins, PROJECT_NE, PROJECT_EW, PROJECT_SE);
        createFreestyleProjectWithShell(jenkins, PROJECT_SW, "rapakalja");

        jenkins.jenkins.setNumExecutors(50);
        //TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary
        jenkins.jenkins.setNodes(jenkins.getInstance().getNodes()); // update nodes configuration

        MatrixProject matrixProject = MatrixSupport.createMatrixProject(jenkins, MATRIX_PROJECT_NEWS);

        AxisList axes = new AxisList();
        axes.add(new Axis("X", "EAST", "WEST"));
        axes.add(new Axis("Y", "NORTH", "SOUTH"));
        matrixProject.setAxes(axes);

        List<AbstractBuildParameters> buildParameters =
                new ArrayList<>();
        buildParameters.add(new CurrentBuildParameters());
        BlockingBehaviour neverFail =
                new BlockingBehaviour("FAILURE", "FAILURE", "UNSTABLE");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig(
                "${Y}-${X}", neverFail, buildParameters);

        matrixProject.getBuildersList().add(new TriggerBuilder(config));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        matrixProject.getBuildersList().add(builder);

        return matrixProject;
    }

    /**
     * Creates several FreeStyleProjects with a basic shell step. Each shell is
     * loaded with a default command: "echo I am ${PROJECT_NAME}"
     *
     * @param jenkins
     * @param names an array of project names
     *
     * @throws Exception if project(s) can't be created
     */
    private static void createFreestyleProjectWithDefaultShell(JenkinsRule jenkins, String... names)
            throws Exception {
        for (String name : names) {
            createFreestyleProjectWithShell(jenkins, name, DEFAULT);
        }
    }

    /**
     * Creates a FreeStyleProject with a basic shell/batch step. The shell is loaded
     * with the supplied command.
     *
     * @param jenkins
     * @param name the name of the project
     * @param command the shell command
     * @return created project
     * @throws Exception if project(s) can't be created
     */
    private static FreeStyleProject createFreestyleProjectWithShell(JenkinsRule jenkins, String name, String command)
            throws Exception {
        final FreeStyleProject project = jenkins.createFreeStyleProject(name);
        if (Functions.isWindows()) {
            project.getBuildersList().add(new BatchFile(command));
        } else {
            project.getBuildersList().add(new Shell(command));
        }
        return project;
    }
}
