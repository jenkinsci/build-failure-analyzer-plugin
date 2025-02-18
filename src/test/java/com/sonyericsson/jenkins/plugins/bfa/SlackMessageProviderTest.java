package com.sonyericsson.jenkins.plugins.bfa;

import jenkins.model.Jenkins;
import jenkins.plugins.slack.SlackNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.apache.http.ssl.SSLInitializationException;
import org.mockito.MockedStatic;

/**
 * Tests for the SlackMessageProvider class.
 * @author Johan Cornelissen &lt;j.cornelissen@queensu.ca&gt;
 */
class SlackMessageProviderTest {
    private SlackNotifier slackPlugin;
    private SlackNotifier.DescriptorImpl descriptor;
    private static final String JENKINS_URL =  "http://some.jenkins.com";
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<SlackNotifier> slackNotifierMockedStatic;
    private MockedStatic<PluginImpl> pluginMockedStatic;

    /**
     * Initialize basic stuff: Jenkins, PluginImpl, etc.
     */
    @BeforeEach
    void setUp() {
        jenkinsMockedStatic = mockStatic(Jenkins.class);
        Jenkins jenkins = mock(Jenkins.class);
        jenkinsMockedStatic.when(Jenkins::get).thenReturn(jenkins);
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
    @AfterEach
    void tearDown() {
        jenkinsMockedStatic.close();
        slackNotifierMockedStatic.close();
        pluginMockedStatic.close();
    }

    /**
     * Test to no slack message is sent if a slack channel is not provided.
     */
    @Test
    void testEmptyChannelSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh", true, "", "");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertFalse(result);
    }

    /**
     * Test to ensure a slack message can use baseUrl if no team domain is provided.
     */
    @Test
    void testEmptyTeamDomainSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "team.slack.com", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertTrue(result);
    }

    /**
     * Test to ensure a slack message can use team domain if no baseUrl is provided.
     */
    @Test
    void testEmptyBaseUrlSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertTrue(result);
    }

    /**
     * Test to ensure a slack message is not sent if both baseUrl and team domain are missing.
     */
    @Test
    void testEmptyBaseUrlAndTeamDomainSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
          //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertFalse(result);
    }

    /**
     * Test to ensure a slack message is not sent if authtoken and authCredentialId is missing.
     */
    @Test
    void testMissingAuthSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("", "team.slack.com", "",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
            //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }

        assertFalse(result);
    }

    /**
     * Test to ensure a slack message is sent if all necessary fields are supplied.
     */
    @Test
    void testSuccessfulSlackMessage() {
        boolean result = false;
        try {
            SlackMessageProvider slackTest = new SlackMessageProvider("team", "", "akasfahjfh",
                    true, "", "#some_channel");
            result = slackTest.postToSlack("Some Message", null);
        } catch (SSLInitializationException e) {
            //SSL error indicates it tried to send to slack, so therefore was "successful"
            result = true;
        }
        assertTrue(result);
    }
}
