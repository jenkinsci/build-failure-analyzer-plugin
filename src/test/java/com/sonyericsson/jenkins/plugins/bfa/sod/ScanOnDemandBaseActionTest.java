/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonyericsson.jenkins.plugins.bfa.sod;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.RunList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.powermock.api.mockito.PowerMockito;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the ScanOnDemandBaseAction.
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 * @throws Exception if so.
 */
public class ScanOnDemandBaseActionTest extends HudsonTestCase {

    private static final String TO_PRINT = "ERROR";
    /**
     * Tests for performScanMethod by passing failed build.
     *
     * @throws Exception if so.
     */
    public void testPerformScanFailedProject() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        assertBuildStatus(Result.FAILURE, build);
        RunList sodbuilds = new RunList();
        sodbuilds.add(build);
        ScanOnDemandBaseAction sodbaseaction = new ScanOnDemandBaseAction(project);
        StaplerRequest mockrequest = PowerMockito.mock(StaplerRequest.class);
        StaplerResponse mockresponse = PowerMockito.mock(StaplerResponse.class);
        sodbaseaction.doPerformScan(mockrequest, mockresponse);
        assertNotNull(build.getAction(FailureCauseBuildAction.class));
    }

    /**
     * Tests for performScanMethod by passing sucess build.
     *
     * @throws Exception if so.
     */
    public void testPerformScanSucessProject() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0);
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        if (build.getAction(FailureCauseBuildAction.class) != null) {
            build.getActions().remove(build.getAction(FailureCauseBuildAction.class));
        }
        assertNull(build.getAction(FailureCauseBuildAction.class));
        assertBuildStatus(Result.SUCCESS, build);
        RunList sodbuilds = new RunList();
        sodbuilds.add(build);
        ScanOnDemandBaseAction sodbaseaction = new ScanOnDemandBaseAction(project);
        StaplerRequest mockrequest = PowerMockito.mock(StaplerRequest.class);
        StaplerResponse mockresponse = PowerMockito.mock(StaplerResponse.class);
        sodbaseaction.doPerformScan(mockrequest, mockresponse);
        assertNull(build.getAction(FailureCauseBuildAction.class));
    }
 }
