package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the GerritMessageProviderExtensionTest.
 * @author Alexander Akbashev mr.akbashev@gmail.com
 * @throws Exception if so.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginImpl.class, Jenkins.class})
public class GerritMessageProviderExtensionTest {
    private static final String JENKINS_URL =  "http://some.jenkins.com";
    private static final String BUILD_URL = "jobs/build/123";

    /**
     * Initialize basic stuff: Jenkins, PluginImpl, etc.
     */
    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);


        PowerMockito.mockStatic(PluginImpl.class);
        PluginImpl plugin = PowerMockito.mock(PluginImpl.class);
        PowerMockito.when(plugin.isGerritTriggerEnabled()).thenReturn(true);
        PowerMockito.when(PluginImpl.getInstance()).thenReturn(plugin);

    }

    /**
     * Test that single quote would be escaped.
     */
    @Test
    public void testSingleQuote() {
        Run run = PowerMockito.mock(Run.class);

        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();
        failureCauses.add(new FoundFailureCause(new FailureCause("testName", "it's a single quote")));
        FailureCauseBuildAction action = new FailureCauseBuildAction(failureCauses);

        PowerMockito.when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        PowerMockito.when(run.getUrl()).thenReturn(BUILD_URL);

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("it\\'s a single quote ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that multiple quotes would be escaped.
     */
    @Test
    public void testMultipleQuotes() {
        Run run = PowerMockito.mock(Run.class);

        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();
        failureCauses.add(new FoundFailureCause(new FailureCause("testName", "s'e'v'e'r'a'l q'u'o't'e's")));
        FailureCauseBuildAction action = new FailureCauseBuildAction(failureCauses);

        PowerMockito.when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        PowerMockito.when(run.getUrl()).thenReturn(BUILD_URL);

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("s\\'e\\'v\\'e\\'r\\'a\\'l q\\'u\\'o\\'t\\'e\\'s ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }
}
