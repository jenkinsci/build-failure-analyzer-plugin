/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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

import jenkins.plugins.slack.SlackNotifier;
import jenkins.plugins.slack.SlackService;
import jenkins.plugins.slack.StandardSlackService;
import jenkins.model.Jenkins;
import java.util.logging.Logger;

import java.io.PrintStream;
import java.util.logging.Level;

/**
 * Class that allows BFA to send failure cause messages for each build to Slack.
 *
 * @author Johan Cornelissen &lt;j.cornelissen@queensu.ca&gt;
 */
public class SlackMessageProvider {
    private static final Logger logger = Logger.getLogger(SlackMessageProvider.class.getName());
    private String slackMessageText;
    private String teamDomain;
    private String baseUrl;
    private String room;
    private String authToken;
    private boolean botUser;
    private String authTokenCredentialId;

    /**
     * Default constructor that uses attributes from Slack Notifier plugin.
     */
    SlackMessageProvider() {
        SlackNotifier.DescriptorImpl slackDesc = getSlackDescriptor();
        this.teamDomain = slackDesc.getTeamDomain();
        this.baseUrl = slackDesc.getBaseUrl();
        this.authToken = slackDesc.getToken();
        this.botUser = slackDesc.getBotUser();
        this.authTokenCredentialId = slackDesc.getTokenCredentialId();
        this.room = slackDesc.getRoom();
    }

    /**
     * Constructor that uses custom attributes.
     * @param teamDomain - Team slack domain (optional if baseUrl specified)
     * @param baseUrl - Slack url (optional if team domain specified)
     * @param authToken - Authorization token (optional if authTokenCredentialId specified)
     * @param botUser - boolean if using bot user.
     * @param authTokenCredentialId - Authorization credential id (optional if authToken specified)
     * @param room - Slack channel
     */
    SlackMessageProvider(String teamDomain, String baseUrl, String authToken, boolean botUser,
            String authTokenCredentialId, String room) {
        this.teamDomain = teamDomain;
        this.baseUrl = baseUrl;
        this.authToken = authToken;
        this.botUser = botUser;
        this.authTokenCredentialId = authTokenCredentialId;
        this.room = room;
    }

    /**
     * Get descriptor for Slack Notifier plugin.
     * @return Descriptor
     */
    private SlackNotifier.DescriptorImpl getSlackDescriptor(){
        return (SlackNotifier.DescriptorImpl)Jenkins.getInstance().getDescriptor(SlackNotifier.class);
    }

    /**
     * Retrieve latest set Slack bot user.
     * @return String of slack bot user
     */
    public boolean getBotUser() {
        return this.botUser;
    }

    /**
     * Retrieve latest set Slack team domain.
     * @return String of slack team domain
     */
    public String getTeamDomain() {
        return this.teamDomain;
    }

    /**
     * Retrieve latest set Slack base URL.
     * @return String of slack base URL
     */
    public String getBaseUrl() {
        return this.baseUrl;
    }

    /**
     * Retrieve latest set Slack channel.
     * @return String of slack channel name
     */
    public String getRoom() {
        return this.room;
    }

    /**
     * Retrieve latest slack message text.
     * @return String of slack message
     */
    public String getSlackMessageText() {
        return this.slackMessageText;
    }

    /**
     * Function to ensure Slack plugin is properly configured.
     * @return Boolean true if configuration is valid, false otherwise
     */
    private boolean checkSlackConfigurationParams() {
        if (room != null && !room.equals("")
                && ((teamDomain != null && !teamDomain.equals("")) || (baseUrl != null && !baseUrl.equals("")))
                && ((authToken != null && !authToken.equals(""))
                        || (authTokenCredentialId != null && !authTokenCredentialId.equals(""))))  {
            return true;
        }
        return false;
    }

    /**
     * Function to report an error message to the plugin logs and to the build console.
     * @param buildLog - PrintStream for current build log
     * @param message - Extra message to append (if not null)
     */
    private void reportGenericErrorMessage(PrintStream buildLog, String message) {
        StringBuilder s = new StringBuilder("Global Slack Notifier tried posting build failure to slack."
                + " However some error occurred\n");
        s.append("TeamDomain :" + teamDomain + "\n");
        s.append("BaseUrl :" + baseUrl + "\n");
        s.append("Channel :" + room + "\n");
        s.append("BotUser:" + botUser);
        if (message != null) {
            s.append("\n"+message);
        }

        if (buildLog != null) {
            buildLog.println("[BFA] Failed to send build failure information to slack.");
            if (message != null) {
                buildLog.println(message);
            }
        }

        logger.log(Level.SEVERE, s.toString());
    }

    /**
     * Function to send a slack message to a channel specificed in
     * the plugin configuration. Uses all slack plugin settings except those
     * that are overridden by the build failure analyzer plugin configurations (this plugin).
     *
     * @param messageText - Content of the slack message.
     * @param buildLog - PrintStream of build to allow for success and error messages to be displayed.
     * @return boolean true if message sent successfully, otherwise false
     */
    public boolean postToSlack(String messageText, PrintStream buildLog) {
        slackMessageText = messageText;
        String messageColor = "danger";
        String useRoom;

        String customSlackChannel = PluginImpl.getInstance().getSlackChannelName();
        if (customSlackChannel.equals("")) {
            // If no slack channel was specified in plugin settings,
            // use channel specified in Global Slack Notifier Plugin settings
            useRoom = this.room;
        } else {
            useRoom = customSlackChannel;
        }

        if (checkSlackConfigurationParams()) {
            SlackService service = new StandardSlackService(baseUrl, teamDomain, authToken,
                    authTokenCredentialId, botUser, useRoom);
            boolean postResult = service.publish(messageText, messageColor);
            if(!postResult){
                reportGenericErrorMessage(buildLog, null);
            } else {
                if (buildLog != null) {
                    buildLog.println("[BFA] Sent build failure information to slack.");
                }
            }
            return true;
        } else {
            reportGenericErrorMessage(buildLog, "Slack Notifier plugin missing configurations.");
            return false;
        }
    }
}
