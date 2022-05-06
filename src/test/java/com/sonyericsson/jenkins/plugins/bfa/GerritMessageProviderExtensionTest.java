package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for the GerritMessageProviderExtensionTest.
 * @author Alexander Akbashev mr.akbashev@gmail.com
 */
public class GerritMessageProviderExtensionTest {
    private static final String JENKINS_URL =  "http://some.jenkins.com";
    private static final String BUILD_URL = "jobs/build/123";
    private static final String NO_CAUSES_MSG = "FEEL_FREE_TO_PUT_WHAT_EVER_YOU_WANT";
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<PluginImpl> pluginMockedStatic;

    /**
     * Initialize basic stuff: Jenkins, PluginImpl, etc.
     */
    @Before
    public void setUp() {
        jenkinsMockedStatic = mockStatic(Jenkins.class);
        Jenkins jenkins = mock(Jenkins.class);
        jenkinsMockedStatic.when(Jenkins::getInstance).thenReturn(jenkins);
        when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);


        pluginMockedStatic = mockStatic(PluginImpl.class);
        PluginImpl plugin = mock(PluginImpl.class);
        when(plugin.isGerritTriggerEnabled()).thenReturn(true);
        pluginMockedStatic.when(PluginImpl::getInstance).thenReturn(plugin);
        when(plugin.getNoCausesMessage()).thenReturn(NO_CAUSES_MSG);
    }

    /**
     * Release all the static mocks.
     */
    @After
    public void tearDown() {
        jenkinsMockedStatic.close();
        pluginMockedStatic.close();
    }

    /**
     *
     * Creates run with desired failure cause
     *
     * @param cause failure cause that would be displayed.
     * @return Run with desired failure cause.
     */
    private Run getRunWithTopCause(String cause) {
        Run run = mock(Run.class);
        Job parent = mock(Job.class);
        FailureCauseBuildAction action = mock(FailureCauseBuildAction.class);

        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();

        final FailureCauseDisplayData displayData = new FailureCauseDisplayData("parentURL", "parentName",
                "/jobs/build/123", "buildName");
        displayData.setFoundFailureCauses(failureCauses);
        if (cause != null) {
            failureCauses.add(new FoundFailureCause(new FailureCause("testName", cause)));
        }

        when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        when(run.getUrl()).thenReturn(BUILD_URL);
        when(run.getParent()).thenReturn(parent);
        when(action.getBuild()).thenReturn(run);
        when(action.getFailureCauseDisplayData()).thenReturn(displayData);
        return run;
    }

    /**
     *
     * Creates run with desired failure cause on third level
     *
     * @param cause failure cause that would be displayed.
     * @return Run with desired failure cause in third level.
     */
    private Run getRunWithDeepCause(String cause) {
        Run run = mock(Run.class);
        Job parent = mock(Job.class);
        FailureCauseBuildAction action = mock(FailureCauseBuildAction.class);

        List<FoundFailureCause> emptyFailureCauses = new ArrayList<FoundFailureCause>();
        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();
        failureCauses.add(new FoundFailureCause(new FailureCause("testName", cause)));

        FailureCauseDisplayData bottomDisplayData = new FailureCauseDisplayData("parentURL", "bottomBuildName",
                "/jobs/build/789", "bottomBuildName");
        bottomDisplayData.setFoundFailureCauses(failureCauses);

        FailureCauseDisplayData middleDisplayData = new FailureCauseDisplayData("parentURL", "middleBuildName",
                "/jobs/build/456", "middleBuildName");
        middleDisplayData.setFoundFailureCauses(emptyFailureCauses);
        middleDisplayData.addDownstreamFailureCause(bottomDisplayData);

        FailureCauseDisplayData topLevelDisplayData = new FailureCauseDisplayData("topParentURL", "topParentName",
                "/jobs/build/123", "topBuildName");
        topLevelDisplayData.setFoundFailureCauses(emptyFailureCauses);
        topLevelDisplayData.addDownstreamFailureCause(middleDisplayData);

        when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        when(run.getUrl()).thenReturn(BUILD_URL);
        when(run.getParent()).thenReturn(parent);
        when(action.getBuild()).thenReturn(run);
        when(action.getFailureCauseDisplayData()).thenReturn(topLevelDisplayData);

        return run;
    }

    /**
     * Test that single quote would be escaped.
     */
    @Test
    public void testSingleQuote() {
        Run run = getRunWithTopCause("it's a single quote");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("it\"s a single quote ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that multiple quotes would be escaped.
     */
    @Test
    public void testMultipleQuotes() {
        Run run = getRunWithTopCause("q'u'o't'e's");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("q\"u\"o\"t\"e\"s ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that message from top build would be displayed.
     */
    @Test
    public void testTopCause() {
        Run run = getRunWithTopCause("some cause");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("some cause ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that message from nested build would be displayed.
     */
    @Test
    public void testDeepCause() {
        Run run = getRunWithDeepCause("deep cause");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("deep cause ( http://some.jenkins.com/jobs/build/789 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that even if there no causes messages will be created.
     */
    @Test
    public void testNoCause() {
        Run run = getRunWithTopCause(null);

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals(NO_CAUSES_MSG + " ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }
}
