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
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.Functions;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Shell;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assume.assumeFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

//CS IGNORE MagicNumber FOR NEXT 260 LINES. REASON: TestData

/**
 * Test fetching display data object for build failure analysis of downstream
 * objects.
 *
 * @author Jan-Olof Sivtoft
 */
public class DisplayDownstreamTest {
    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private MatrixProject matrixProject;

    private static final String MATRIX_PROJECT_NEWS = "NEWS";
    private static final String PROJECT_NE = "NORTH-EAST";
    private static final String PROJECT_EW = "NORTH-WEST";
    private static final String PROJECT_SE = "SOUTH-EAST";
    private static final String PROJECT_SW = "SOUTH-WEST";
    private static final String DEFAULT = "echo I am ${PROJECT_NAME}";
    private static final String FAILED = "rapakalja";

    /**
     * Test the FailureCauseDisplayData object.
     *
     * @throws Exception if failure  build can't be executed
     */
    @Test
    public void testFailureCauseDisplayData() throws Exception {
        assumeFalse(Functions.isWindows());

        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild());

        assertNotNull(failureCauseDisplayData.getDownstreamFailureCauses());
        assertNotNull(failureCauseDisplayData.getFoundFailureCauses());

        FailureCauseDisplayData.Links links =
                failureCauseDisplayData.getLinks();
        assertNotNull(links);

        assertEquals("WEST,SOUTH", links.getProjectDisplayName());
        assertNotNull(links.getProjectUrl());
        assertEquals("#1", links.getBuildDisplayName());
        assertNotNull(links.getBuildUrl());
    }


    /**
     * Test FailureCauseDisplayData object population when no indication is
     * specified.
     *
     * @throws Exception if failure build can't be executed
     */
    @Test
    public void testMatrixNoIdentifiedCause() throws Exception {
        assumeFalse(Functions.isWindows());

        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild());

        // It is not the matrix run that fails
        assertTrue(failureCauseDisplayData.getFoundFailureCauses().size() == 0);
        assertTrue(failureCauseDisplayData.
                getDownstreamFailureCauses().size() == 1);

        FailureCauseDisplayData downstreamFailureCauseDisplayData =
                failureCauseDisplayData.getDownstreamFailureCauses().
                        get(0);

        List<FoundFailureCause> causeListFromAction =
                downstreamFailureCauseDisplayData.getFoundFailureCauses();

        // No indication added so this is expected
        assertTrue(causeListFromAction.size() == 0);

        assertEquals(PROJECT_SW, downstreamFailureCauseDisplayData.
                getLinks().getProjectDisplayName());
    }

    /**
     * Test FailureCauseDisplayData object population when an indication is
     * specified.
     *
     * @throws Exception if failure cause cant be configured or build can't
     *                   be executed
     */
    @Test
    public void testMatrixIdentifiedCause() throws Exception {
        assumeFalse(Functions.isWindows());

        Indication indication = new BuildLogIndication(".*" + FAILED + ".*");
        FailureCause failureCause = BuildFailureScannerHudsonTest.
                configureCauseAndIndication("Other cause", "Other description", "Other comment",
                        "Category", indication);

        FailureCauseDisplayData failureCauseDisplayData =
                getDisplayData(executeBuild());

        // It is not the matrix run that fails
        assertTrue(failureCauseDisplayData.getFoundFailureCauses().size() == 0);
        assertTrue(failureCauseDisplayData.
                getDownstreamFailureCauses().size() == 1);

        FailureCauseDisplayData downstreamFailureCauseDisplayData =
                failureCauseDisplayData.getDownstreamFailureCauses().
                        get(0);

        List<FoundFailureCause> causeListFromAction =
                downstreamFailureCauseDisplayData.getFoundFailureCauses();

        assertTrue(causeListFromAction.size() == 1);

        // This is the expected indication
        assertTrue(BuildFailureScannerHudsonTest.findCauseInList(
                causeListFromAction, failureCause));

        assertEquals(PROJECT_SW, downstreamFailureCauseDisplayData.getLinks().
                getProjectDisplayName());
    }

    /**
     * Test FailureCauseDisplayData object population when an indication is
     * specified.
     *
     * @throws Exception if failure cause cant be configured or build can't
     *                   be executed
     */
    @Test
    public void testIdentifiedTwoCauses() throws Exception {
        assumeFalse(Functions.isWindows());

        final FreeStyleProject child1 = createFreestyleProjectWithShell("child1", FAILED);
        final FreeStyleProject child2 = createFreestyleProjectWithShell("child2", FAILED);

        final FreeStyleProject parent = jenkins.createFreeStyleProject("parent");
        parent.getBuildersList().add(new TriggerBuilder(
                new BlockableBuildTriggerConfig(child1.getName() + ", " + child2.getName(),
                    new BlockingBehaviour(Result.FAILURE, Result.FAILURE, Result.FAILURE),
                    new ArrayList<AbstractBuildParameters>())));

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
            assertEquals(causeListFromAction.get(0).getName(), "Other cause");
        }
    }

    /**
     * Creates and executes a matrix build.
     *
     * @return a Matrix build object
     * @throws Exception if build couldn't be executed
     */
    private MatrixBuild executeBuild() throws Exception {

        createMatrixProjectNews();

        matrixProject.setQuietPeriod(0);
        jenkins.getInstance().rebuildDependencyGraph();
        matrixProject.scheduleBuild2(0, new Cause.UserCause()).get();

        return matrixProject.getLastBuild();
    }

    /**
     * Returns FailureCauseDisplayData form the first MatrixBuild run
     *
     * @param build the executed build
     * @return a FailureCauseDisplayData object
     */
    private FailureCauseDisplayData getDisplayData(MatrixBuild build) {

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
     * @throws Exception if project(s) can't be created
     */
    private void createMatrixProjectNews() throws Exception {

        createFreestyleProjectWithDefaultShell(
                PROJECT_NE, PROJECT_EW, PROJECT_SE);
        createFreestyleProjectWithShell(PROJECT_SW, "rapakalja");

        jenkins.getInstance().setNumExecutors(50);
        //TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary
        jenkins.getInstance().setNodes(jenkins.getInstance().getNodes()); // update nodes configuration

        matrixProject = jenkins.createMatrixProject(MATRIX_PROJECT_NEWS);

        AxisList axes = new AxisList();
        axes.add(new Axis("X", "EAST", "WEST"));
        axes.add(new Axis("Y", "NORTH", "SOUTH"));
        matrixProject.setAxes(axes);

        List<AbstractBuildParameters> buildParameters =
                new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());
        BlockingBehaviour neverFail =
                new BlockingBehaviour("FAILURE", "FAILURE", "UNSTABLE");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig(
                "${Y}-${X}", neverFail, buildParameters);

        matrixProject.getBuildersList().add(new TriggerBuilder(config));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        matrixProject.getBuildersList().add(builder);
    }

    /**
     * Creates several FreeStyleProjects with a basic shell step. Each shell is
     * loaded with a default command: "echo I am ${PROJECT_NAME}"
     *
     * @param names an array of project names
     * @throws Exception if project(s) can't be created
     */
    private void createFreestyleProjectWithDefaultShell(String... names)
            throws Exception {
        for (String name : names) {
            createFreestyleProjectWithShell(name, DEFAULT);
        }
    }

    /**
     * Creates a FreeStyleProject with a basic shell step. The shell is loaded
     * with the supplied command.
     *
     * @param name the name of the project
     * @param command the shell command
     * @return created project
     * @throws Exception if project(s) can't be created
     */
    private FreeStyleProject createFreestyleProjectWithShell(String name, String command)
            throws Exception {
        final FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.getBuildersList().add(new Shell(command));
        return project;
    }

}
