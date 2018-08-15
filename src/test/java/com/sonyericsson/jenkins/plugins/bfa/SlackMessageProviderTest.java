package com.sonyericsson.jenkins.plugins.bfa;

import jenkins.model.Jenkins;
import jenkins.plugins.slack.SlackNotifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

import org.apache.http.ssl.SSLInitializationException;

/**
 * Tests for the SlackMessageProvider class.
 * @author Johan Cornelissen &lt;j.cornelissen@queensu.ca&gt;
 * @throws Exception if so.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginImpl.class, SlackNotifier.class, Jenkins.class})
public class SlackMessageProviderTest {
    private SlackNotifier slackPlugin;
    private SlackNotifier.DescriptorImpl descriptor;
    private static final String JENKINS_URL =  "http://some.jenkins.com";

    /**
     * Initialize basic stuff: Jenkins, PluginImpl, etc.
     */
    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);

        PowerMockito.mockStatic(SlackNotifier.class);
        slackPlugin = PowerMockito.mock(SlackNotifier.class);

        PowerMockito.mockStatic(PluginImpl.class);
        PluginImpl plugin = PowerMockito.mock(PluginImpl.class);
        PowerMockito.when(plugin.isSlackNotifEnabled()).thenReturn(true);
        PowerMockito.when(PluginImpl.getInstance()).thenReturn(plugin);
        PowerMockito.when(PluginImpl.getInstance().getSlackChannelName()).thenReturn("");
    }

    /**
     * Test to no slack message is sent if a slack channel is not provided.
     * @throws Exception if so.
     */
    @Test
    public void testEmptyChannelSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh", true, "", "");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertEquals(false, result);
    }

    /**
     * Test to ensure a slack message can use baseUrl if no team domain is provided.
     * @throws Exception if so.
     */
    @Test
    public void testEmptyTeamDomainSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "team.slack.com", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertEquals(true, result);
    }

    /**
     * Test to ensure a slack message can use team domain if no baseUrl is provided.
     * @throws Exception if so.
     */
    @Test
    public void testEmptyBaseUrlSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertEquals(true, result);
    }

    /**
     * Test to ensure a slack message is not sent if both baseUrl and team domain are missing.
     * @throws Exception if so.
     */
    @Test
    public void testEmptyBaseUrlAndTeamDomainSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertEquals(false, result);
    }

    /**
     * Test to ensure a slack message is not sent if authtoken and authCredentialId is missing.
     * @throws Exception if so.
     */
    @Test
    public void testMissingAuthSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "team.slack.com", "",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
            //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertEquals(false, result);
    }

    /**
     * Test to ensure a slack message is sent if all necessary fields are supplied.
     * @throws Exception if so.
     */
    @Test
    public void testSuccessfulSlackMessage() throws Exception {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
            //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }
        assertEquals(true, result);
    }
}
