package com.sonyericsson.jenkins.plugins.bfa;

import jenkins.model.Jenkins;
import jenkins.plugins.slack.SlackNotifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.apache.http.ssl.SSLInitializationException;
import org.mockito.MockedStatic;

/**
 * Tests for the SlackMessageProvider class.
 * @author Johan Cornelissen &lt;j.cornelissen@queensu.ca&gt;
 */
public class SlackMessageProviderTest {
    private SlackNotifier slackPlugin;
    private SlackNotifier.DescriptorImpl descriptor;
    private static final String JENKINS_URL =  "http://some.jenkins.com";
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<SlackNotifier> slackNotifierMockedStatic;
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

        slackNotifierMockedStatic = mockStatic(SlackNotifier.class);
        slackPlugin = mock(SlackNotifier.class);

        pluginMockedStatic = mockStatic(PluginImpl.class);
        PluginImpl plugin = mock(PluginImpl.class);
        when(plugin.isSlackNotifEnabled()).thenReturn(true);
        pluginMockedStatic.when(PluginImpl::getInstance).thenReturn(plugin);
        when(plugin.getSlackChannelName()).thenReturn("");
    }

    /**
     * Release all the static mocks.
     */
    @After
    public void tearDown() {
        jenkinsMockedStatic.close();
        slackNotifierMockedStatic.close();
        pluginMockedStatic.close();
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
