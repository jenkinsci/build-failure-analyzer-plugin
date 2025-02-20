/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications Inc. All rights reserved.
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

import com.codahale.metrics.MetricRegistry;
import hudson.model.Run;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import com.sonyericsson.hudson.plugins.gerrit.trigger.gerritnotifier.ToGerritRunListener;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.listeners.RunListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.junit.JUnitResultArchiver;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//CS IGNORE MagicNumber FOR NEXT 1500 LINES. REASON: TestData.

/**
 * Tests for the FailureScanner.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@WithJenkins
public class BuildFailureScannerHudsonTest {

    private static final String BUILD_LOG = "ERROR: brief\n  detail\n";
    private static final String BUILD_LOG_FIRST_LINE = "ERROR: brief";
    private static final String DESCRIPTION = "The error was: ${1,1}${2,1}";
    private static final String REGEX = "ERROR: (.*?)$";
    private static final String MULTILINE_REGEX = "ERROR: (.*?)$.*?  detail";
    private static final String FORMATTED_DESCRIPTION = "The error was: brief";

    private boolean hasCalledStatistics = false;

    /**
     * Happy test that should find one failure indication in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOneIndicationFound(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        FailureCause failureCause = configureCauseAndIndication();

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        ScanLogAction scanLogAction = build.getAction(ScanLogAction.class);
        assertNotNull(scanLogAction);
        assertNotNull(scanLogAction.getStartTime());
        assertNotNull(scanLogAction.getEndTime());
        assertNull(scanLogAction.getExceptionMessage());

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));

        HtmlPage page = jenkins.createWebClient().goTo(build.getUrl() + "console");
        HtmlElement document = page.getDocumentElement();

        FoundFailureCause foundFailureCause = causeListFromAction.get(0);
        assertEquals(FORMATTED_DESCRIPTION, foundFailureCause.getDescription());
        FoundIndication foundIndication = foundFailureCause.getIndications().get(0);
        String id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        DomElement focus = page.getElementById(id);
        assertNotNull(focus);

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", foundFailureCause.getName());
        assertNotNull(errorElements);
        HtmlElement error = errorElements.get(0);

        assertNotNull(error);
        assertEquals(BUILD_LOG_FIRST_LINE, error.getTextContent().trim(), "Error message not found: ");

        MetricRegistry metricRegistry = Metrics.metricRegistry();
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Error").getCount());
    }

    /**
     * Test when an exception occurred during scan.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testExceptionDuringParsing(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        FailureCause failureCause = configureCauseAndIndication(new BuildLogIndication("(wrong pattern"));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        ScanLogAction scanLogAction = build.getAction(ScanLogAction.class);
        assertNotNull(scanLogAction);
        assertNotNull(scanLogAction.getStartTime());
        assertNotNull(scanLogAction.getEndTime());
        assertNotNull(scanLogAction.getExceptionMessage());

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
    }

    /**
     * Happy test that should find one generic failure indication in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOnlyOneGenericIndicationFound(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setFallbackCategoriesAsString("Generic");

        FailureCause genericFailureCause = configureCauseAndIndication(
                "Generic Error", "an error", "", "Generic", new BuildLogIndication(".*Generic Error.*")
        );
        FailureCause specificFailureCause = configureCauseAndIndication(
                "Specific Error", "an error", "", "Specific", new BuildLogIndication(".*Specific Error.*")
        );

        FreeStyleProject project = createProject(jenkins, "Generic Error\nUnknown Error");

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        List<FoundFailureCause> causeListFromAction = build
                .getAction(FailureCauseBuildAction.class)
                .getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, genericFailureCause));
        assertFalse(findCauseInList(causeListFromAction, specificFailureCause));

        MetricRegistry metricRegistry = Metrics.metricRegistry();
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Generic Error").getCount());
        assertEquals(1, metricRegistry.counter("jenkins_bfa.category.Generic").getCount());
        assertEquals(0, metricRegistry.counter("jenkins_bfa.cause.Specific Error").getCount());
        assertEquals(0, metricRegistry.counter("jenkins_bfa.category.Specific").getCount());
    }

    /**
     * Happy test that should replace the generic failure indication with the specific one.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testGenericFailureCauseIsDroppedForSpecificOne(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setFallbackCategoriesAsString("Generic");

        FailureCause genericFailureCause = configureCauseAndIndication(
                "Generic Error", "an error", "", "Generic", new BuildLogIndication(".*Generic Error.*")
        );
        FailureCause specificFailureCause = configureCauseAndIndication(
                "Specific Error", "an error", "", "Specific", new BuildLogIndication(".*Specific Error.*")
        );

        FreeStyleProject project = createProject(jenkins, "Generic Error\nSpecific Error");

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        List<FoundFailureCause> causeListFromAction = build
                .getAction(FailureCauseBuildAction.class)
                .getFoundFailureCauses();
        assertFalse(findCauseInList(causeListFromAction, genericFailureCause));
        assertTrue(findCauseInList(causeListFromAction, specificFailureCause));

        MetricRegistry metricRegistry = Metrics.metricRegistry();
        assertEquals(0, metricRegistry.counter("jenkins_bfa.cause.Generic Error").getCount());
        assertEquals(0, metricRegistry.counter("jenkins_bfa.category.Generic").getCount());
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Specific Error").getCount());
        assertEquals(1, metricRegistry.counter("jenkins_bfa.category.Specific").getCount());
    }

    /**
     * Happy test that should find one failure indication in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOneMultilineIndicationFound(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        FailureCause failureCause = configureCauseAndIndication(new MultilineBuildLogIndication(MULTILINE_REGEX));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));

        HtmlPage page = jenkins.createWebClient().goTo(build.getUrl() + "console");
        HtmlElement document = page.getDocumentElement();

        FoundFailureCause foundFailureCause = causeListFromAction.get(0);
        assertEquals(FORMATTED_DESCRIPTION, foundFailureCause.getDescription());
        FoundIndication foundIndication = foundFailureCause.getIndications().get(0);
        String id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        DomElement focus = page.getElementById(id);
        assertNotNull(focus);

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", foundFailureCause.getName());
        assertNotNull(errorElements);
        HtmlElement error = errorElements.get(0);

        assertNotNull(error);
        assertEquals(new StringTokenizer(BUILD_LOG).nextToken("\n"),
                error.getTextContent().trim(),
                "Error message not found: ");

        MetricRegistry metricRegistry = Metrics.metricRegistry();
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Error").getCount());
        assertEquals(1, metricRegistry.counter("jenkins_bfa.category.category").getCount());
    }

    /**
     * Happy test that should find two failure causes in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testTwoIndicationsSameLine(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        FailureCause failureCause = configureCauseAndIndication();

        Indication indication = new BuildLogIndication(REGEX);
        final String otherDescription = "Other description";
        FailureCause failureCause2 = configureCauseAndIndication("Other cause", otherDescription, indication);

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));
        assertTrue(findCauseInList(causeListFromAction, failureCause2));

        HtmlPage page = jenkins.createWebClient().goTo(build.getUrl() + "console");

        HtmlElement document = page.getDocumentElement();

        final Set<String> causeDescriptions = new HashSet<>();
        causeDescriptions.add(FORMATTED_DESCRIPTION);
        causeDescriptions.add(otherDescription);

        FoundFailureCause foundFailureCause = causeListFromAction.get(0);
        String description = foundFailureCause.getDescription();
        assertTrue(causeDescriptions.remove(description));
        FoundIndication foundIndication = foundFailureCause.getIndications().get(0);
        String id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        DomElement focus = page.getElementById(id);
        assertNotNull(focus);

        foundFailureCause = causeListFromAction.get(1);
        description = foundFailureCause.getDescription();
        assertTrue(causeDescriptions.remove(description));
        foundIndication = foundFailureCause.getIndications().get(0);
        id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        focus = page.getElementById(id);
        assertNotNull(focus);
        assertTrue(causeDescriptions.isEmpty());

        String title = failureCause.getName() + "\n" + failureCause2.getName();

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", title);
        //The titles could be in any given order, trying both orders before failing.
        if (errorElements.isEmpty()) {
            title = failureCause2.getName() + "\n" + failureCause.getName();
            errorElements = document.getElementsByAttribute("span", "title", title);
        }
        assertFalse(errorElements.isEmpty(), "Title not found in annotated text");
        HtmlElement error = errorElements.get(0);
        assertNotNull(error);
        assertEquals(BUILD_LOG_FIRST_LINE, error.getTextContent().trim(), "Error message not found: ");

        MetricRegistry metricRegistry = Metrics.metricRegistry();
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Error").getCount());
        assertEquals(1, metricRegistry.counter("jenkins_bfa.cause.Other cause").getCount());
        assertEquals(2, metricRegistry.counter("jenkins_bfa.category.category").getCount());
    }

    /**
     * Test to ensure no failure category can be specified, and if all build failures
     * are to be reported, a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoCategoryALLSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories = List.of("ALL");
        PrintStream buildLog = null;

        String result = BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* []",
                "*Description:* Some Description",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure no failure Description can be specified,
     * are to be reported, a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoDesriptionSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "", "", "env");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories = List.of("");
        PrintStream buildLog = null;

        String result = BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* ",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure no failure category can be specified,
     * are to be reported, a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoCategorySlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories = List.of("");
        PrintStream buildLog = null;

        String result = BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* []",
                "*Description:* Some Description",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure one failure category can be specified, and if all build failures
     * are to be reported, a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testSingleCategoryALLSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories = List.of("ALL");
        PrintStream buildLog = null;

        String result = BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure multiple failure categories can be specified, and if at least one of the categories
     * match a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testMultiCategoryFailureSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause testFailureCause1 = new FoundFailureCause(testFailureCause);
        testFailureCause = new FailureCause("Some Fail Cause git", "Some Description git", "", "git");
        FoundFailureCause testFailureCause2 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = Arrays.asList(testFailureCause1, testFailureCause2);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories =  Arrays.asList("Some Fail Cause",
                "Some Fail Cause git");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);

        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                "",
                "*Failure Name:* Some Fail Cause git",
                "*Failure Categories:* [git]",
                "*Description:* Some Description git",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure multiple failure categories can be specified, and if at least one of the categories
     * match a new Slack message should be created.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testMultiCategoryFailureSlackMessageALL(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause testFailureCause1 = new FoundFailureCause(testFailureCause);
        testFailureCause = new FailureCause("Some Fail Cause git", "Some Description git", "", "git");
        FoundFailureCause testFailureCause2 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = Arrays.asList(testFailureCause1, testFailureCause2);

        boolean notifySlackOfAllFailures = true;
        List<String> slackFailureCauseCategories = List.of("ALL");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);

        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                "",
                "*Failure Name:* Some Fail Cause git",
                "*Failure Categories:* [git]",
                "*Description:* Some Description git",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure multiple failure categories can be specified, and if not all the categories
     * match a new Slack message should be created with only the selected category.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testMultiCategoryFailureSlackMessageOnlySelected(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause testFailureCause1 = new FoundFailureCause(testFailureCause);
        testFailureCause = new FailureCause("Some Fail Cause git", "Some Description", "", "git");
        FoundFailureCause testFailureCause2 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = Arrays.asList(testFailureCause1, testFailureCause2);

        boolean notifySlackOfAllFailures = false;
        List<String> slackFailureCauseCategories = List.of("env");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);

        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );

        assertEquals(expected, result);
    }

    /**
     * Test to ensure single failure category can be specified, and if none of the categories
     * match no new Slack message should be created.
     *
     * @param jenkins
     */
    @Test
    void testSingleNonChosenCategoryFailureSlackMessage(JenkinsRule jenkins) {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause testFailureCause1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(testFailureCause1);

        boolean notifySlackOfAllFailures = false;
        List<String> slackFailureCauseCategories = List.of("git");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);

        assertNull(result);
    }

    /**
     * Test to ensure multiple failure categories can be specified, and if none of the categories
     * match no new Slack message should be created.
     *
     * @param jenkins
     */
    @Test
    void testMultiNonChosenCategoryFailureSlackMessage(JenkinsRule jenkins) {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause testFailureCause1 = new FoundFailureCause(testFailureCause);
        testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "code");
        FoundFailureCause testFailureCause2 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = Arrays.asList(testFailureCause1, testFailureCause2);

        boolean notifySlackOfAllFailures = false;
        List<String> slackFailureCauseCategories = List.of("git");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);

        assertNull(result);
    }

    /**
     * Test to ensure a single failure cause category can be chosen in the slack configuration
     * section of the BFA plugin.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testSingleChosenCategoryFailureSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause", "Some Description", "", "env");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = false;
        List<String> slackFailureCauseCategories = List.of("env");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );
        assertEquals(expected, result);
    }

    /**
     * Test to ensure a single failure cause category can be chosen in the slack configuration
     * section of the BFA plugin.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testMultiLineDescriptionSlackMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setSlackNotifEnabled(true);
        FailureCause testFailureCause = new FailureCause("Some Fail Cause",
                "Some Description\nwith multiple lines\nTo test that it works", "", "env");
        FoundFailureCause test1 = new FoundFailureCause(testFailureCause);
        List<FoundFailureCause> foundCauseList = List.of(test1);

        boolean notifySlackOfAllFailures = false;
        List<String> slackFailureCauseCategories = List.of("env");
        PrintStream buildLog = null;

        String result =  BuildFailureScanner.createSlackMessage(foundCauseList, notifySlackOfAllFailures,
                slackFailureCauseCategories, "Sandbox", "#1",
                Jenkins.get().getRootUrl() + "job/test0/1/", buildLog);
        String expected = String.join("\n",
                "Job *\"Sandbox\"* build *##1* FAILED due to following failure causes: ",
                "*Failure Name:* Some Fail Cause",
                "*Failure Categories:* [env]",
                "*Description:* Some Description",
                "with multiple lines",
                "To test that it works",
                String.format("See %sjob/test0/1/ for details.", jenkins.getURL())
        );
        assertEquals(expected, result);
    }

    /**
     * One indication should be found and a correct looking Gerrit-Trigger-Plugin message can be constructed.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOneIndicationBuildCompletedMessage(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setGerritTriggerEnabled(true);

        FreeStyleProject project = createProject(jenkins);

        configureCauseAndIndication();

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        GerritMessageProviderExtension messageProvider = new GerritMessageProviderExtension();

        assertEquals(FORMATTED_DESCRIPTION + " ( " + Jenkins.get().getRootUrl() + "job/test0/1/ )",
                messageProvider.getBuildCompletedMessage((Run)build),
                "The " + GerritMessageProviderExtension.class.getSimpleName()
                + " extension would not return the expected message.");

        PluginImpl.getInstance().setGerritTriggerEnabled(false);

        assertNull(messageProvider.getBuildCompletedMessage((Run)build),
                "The " + GerritMessageProviderExtension.class.getSimpleName()
                + " extension would not return null.");
    }

    /**
     * Unhappy test that should not find any failure indications in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoIndicationFound(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        configureCauseAndIndication(new BuildLogIndication(".*something completely different.*"));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        assertTrue(action.getFoundFailureCauses().isEmpty());
    }

    /**
     * Unhappy test that should not find any failure multiline indications in the build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoMultilineIndicationFound(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);

        configureCauseAndIndication(new MultilineBuildLogIndication(".*something completely different.*"));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        assertTrue(action.getFoundFailureCauses().isEmpty());
    }

    /**
     * Tests that "no problem identified" message is not shown when no failure cause has been found.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testNoIndicationMessageShownIfNoCausesDisabled(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = createProject(jenkins);
        PluginImpl.getInstance().setNoCausesEnabled(false);

        configureCauseAndIndication(new BuildLogIndication(".*something completely different.*"));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        assertTrue(action.getFoundFailureCauses().isEmpty());

        HtmlPage page = jenkins.createWebClient().goTo(build.getUrl());
        HtmlElement document = page.getDocumentElement();
        HtmlElement heading = document.getFirstByXPath("//h4[text()='No identified problem']");
        assertNull(heading);
    }

    /**
     * Makes sure that the build action is not added to a successful build.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testSuccessfulBuild(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(BUILD_LOG));
        configureCauseAndIndication();

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when the the global setting {@link PluginImpl#isGlobalEnabled()} is false.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoNotScanGlobal(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(false);
        FreeStyleProject project = createProject(jenkins);
        configureCauseAndIndication();
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when build log size exceeds max log size.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoNotScanIfLogSizeExceedsLimit(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setMaxLogSize(1);
        FreeStyleProject project = createProject(jenkins, createHugeString(1024 * 1024) + BUILD_LOG);
        configureCauseAndIndication();
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that scanner result presents when build log size is less than max log size.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoScanIfLogSizeIsInLimit(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setMaxLogSize(2);
        FreeStyleProject project = createProject(jenkins, createHugeString(1024 * 1024) + BUILD_LOG);
        configureCauseAndIndication();
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
    }


    /**
     * Create a string with any length than contains only 'a' letters
     *
     * @param length desired string length
     * @return string
     */
    private static String createHugeString(int length)  {
        char[] text = new char[length];
        Arrays.fill(text, 'a');
        return new String(text);
    }

    /**
     * Tests that there is no scanner result when the property
     * {@link com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty}
     * is set to true.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoNotScanSpecific(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(true);
        FreeStyleProject project = createProject(jenkins);
        project.addProperty(new ScannerJobProperty(true));
        configureCauseAndIndication();
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }


    /**
     * Tests that the saveStatistics method of KnowledgeBase is called with a Statistics object.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testStatisticsLogging(JenkinsRule jenkins) throws Exception {

        Indication indication = new BuildLogIndication(REGEX);
        List<Indication> indicationList = new LinkedList<>();
        indicationList.add(indication);
        FailureCause cause = new FailureCause("myId", "testcause", "testdescription", "testcomment",
                                              null, "testcategory", indicationList, null);
        List<FailureCause> causes = new LinkedList<>();
        causes.add(cause);
        KnowledgeBase base = mock(KnowledgeBase.class);
        when(base.isEnableStatistics()).thenReturn(true);
        when(base.getCauses()).thenReturn(causes);
        when(base.isEnableStatistics()).thenReturn(true);
        doAnswer(invocation -> {
            hasCalledStatistics = true;
            return null;
        }).when(base).saveStatistics(ArgumentMatchers.any());
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, base);
        FreeStyleProject project = createProject(jenkins);
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        long time = System.currentTimeMillis();
        while (System.currentTimeMillis() < time + 30000) {
            if (hasCalledStatistics) {
                break;
            }
            final int twoSeconds = 2000;
            Thread.sleep(twoSeconds);
        }
        verify(base).saveStatistics(argThat(new IsValidStatisticsObject()));
    }

    /**
     * Tests that {@link BuildFailureScanner} is found before
     * {@link com.sonyericsson.hudson.plugins.gerrit.trigger.gerritnotifier.ToGerritRunListener}.
     *
     * @param jenkins
     */
    @Test
    void testOrdinal(JenkinsRule jenkins) {
        int counter = 0;
        int bfaPlacement = 0;
        int gtPlacement = 0;
        boolean bfaFound = false;
        boolean gtFound = false;

        for (RunListener listener : RunListener.all()) {
            if (listener instanceof BuildFailureScanner) {
                bfaFound = true;
                bfaPlacement = (counter++);
            } else if (listener instanceof ToGerritRunListener) {
                gtFound = true;
                gtPlacement = (counter++);
            }
        }
        assertTrue(gtFound);
        assertTrue(bfaFound);
        assertTrue(bfaPlacement < gtPlacement, "BFA (" + bfaPlacement + ") should list before GT (" + gtPlacement + ")");
    }

    /**
     * Test whether failed test cases are successfully matched as failure causes.
     *
     * @param jenkins
     *
     * @throws Exception if not so.
     */
    @Test
    void testTestResultInterpretation(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setTestResultParsingEnabled(true);
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.getBuildersList().add(new PrintToLogBuilder(BUILD_LOG));
        project.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                    BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    this.getClass().getResource("junit.xml"));
                return true;
            }
        });

        project.getPublishersList().add(new JUnitResultArchiver("junit.xml", false, null));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);

        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals(2, causeListFromAction.size(), "Amount of failure causes does not match.");

        assertEquals("AFailingTest", causeListFromAction.get(0).getName());
        assertEquals("Here are details of the failure...", causeListFromAction.get(0).getDescription());
        assertEquals(new ArrayList<String>(), causeListFromAction.get(0).getCategories());
        assertEquals("AnotherFailingTest", causeListFromAction.get(1).getName());
        assertEquals("More details", causeListFromAction.get(1).getDescription());
        assertEquals(new ArrayList<String>(), causeListFromAction.get(1).getCategories());
    }

    /**
     * Test whether failed test cases are registered against the configured categories.
     *
     * @param jenkins
     *
     * @throws Exception if not so.
     */
    @Test
    void testTestResultInterpretationWithCategories(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setTestResultParsingEnabled(true);
        String categories = "foo bar";
        PluginImpl.getInstance().setTestResultCategories(categories);
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.getBuildersList().add(new PrintToLogBuilder(BUILD_LOG));
        project.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                    BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    this.getClass().getResource("junit.xml"));
                return true;
            }
        });

        project.getPublishersList().add(new JUnitResultArchiver("junit.xml", false, null));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);

        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals(2, causeListFromAction.size(), "Amount of failure causes does not match.");

        List<String> categoriesList = Arrays.asList(categories.split("\\s+"));
        assertEquals(categoriesList, causeListFromAction.get(0).getCategories());
        assertEquals(categoriesList, causeListFromAction.get(1).getCategories());
    }

    /**
     * Test whether failed test cases are not detected when feature is disabled.
     *
     * @param jenkins
     *
     * @throws Exception if not so.
     */
    @Test
    void testTestResultInterpretationIfDisabled(JenkinsRule jenkins) throws Exception {
        PluginImpl.getInstance().setTestResultParsingEnabled(false);
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.getBuildersList().add(new PrintToLogBuilder(BUILD_LOG));
        project.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                    BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    this.getClass().getResource("junit.xml"));
                return true;
            }
        });

        project.getPublishersList().add(new JUnitResultArchiver("junit.xml", false, null));

        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals(0, causeListFromAction.size(), "Amount of failure causes does not match.");
    }

    /**
     * ArgumentMatcher for a Statistics object.
     */
    public static class IsValidStatisticsObject implements ArgumentMatcher<Statistics> {
        @Override
        public boolean matches(Statistics o) {
            if (o == null) {
                return false;
            }
            if (o.getBuildNumber() != 1) {
                return false;
            }
            List<FailureCauseStatistics> failureCauseStatisticsList = o.getFailureCauseStatisticsList();
            if (failureCauseStatisticsList == null || failureCauseStatisticsList.size() != 1) {
                return false;
            }
            return "myId".equals(failureCauseStatisticsList.get(0).getId());
        }
    }

    /**
     * Convenience method for a standard cause that finds {@link #BUILD_LOG} in the build log.
     *
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     * @see #configureCauseAndIndication(String, String,
     *        com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private static FailureCause configureCauseAndIndication() throws Exception {
        return configureCauseAndIndication(new BuildLogIndication(REGEX));
    }

    /**
     * Convenience method for the standard cause with a special indication.
     *
     * @param indication the indication for the cause.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(String, String,
     *           com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private static FailureCause configureCauseAndIndication(Indication indication) throws Exception {
        return configureCauseAndIndication("Error", DESCRIPTION, indication);
    }

    /**
     * Convenience method for a standard cause with a category and the provided indication.
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     */
    private static FailureCause configureCauseAndIndication(String name, String description, Indication indication)
            throws Exception {
        return configureCauseAndIndication(name, description, "comment", "category", indication);
    }

    /**
     * Configures the global settings with a cause that has the provided indication.
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param comment     the comment of this cause.
     * @param category    the category of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     */
    public static FailureCause configureCauseAndIndication(String name, String description, String comment,
                                                           String category, Indication indication) throws Exception {
        List<Indication> indicationList = new LinkedList<>();
        indicationList.add(indication);
        FailureCause failureCause =
                new FailureCause(name, name, description, comment, null, category, indicationList, null);

        Collection<FailureCause> causes = PluginImpl.getInstance().getKnowledgeBase().getCauses();

        List<FailureCause> causeList = new LinkedList<>(causes);
        causeList.add(failureCause);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(causeList));
        return failureCause;
    }

    /**
     * Creates a project that prints {@link #BUILD_LOG} to the console and fails the build.
     *
     * @param jenkins
     *
     * @return the project
     *
     * @throws IOException if so.
     */
    private static FreeStyleProject createProject(JenkinsRule jenkins) throws IOException {
        return createProject(jenkins, BUILD_LOG);
    }

    /**
     * Creates a project that prints the given log string to the console and fails the build.
     *
     * @param jenkins
     * @param logString the string to appear in the build log
     *
     * @return the project
     *
     * @throws IOException if so
     */
    private static FreeStyleProject createProject(JenkinsRule jenkins, String logString) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(logString));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        return project;
    }

    /**
     * Searches the list for the FailureCause.
     *
     * @param causeListFromAction the list.
     * @param failureCause        the cause.
     * @return true if found, false if not.
     */
    public static boolean findCauseInList(List<FoundFailureCause> causeListFromAction, FailureCause failureCause) {
        for (FoundFailureCause cause : causeListFromAction) {
            if (failureCause.getName().equals(cause.getName())
                    && failureCause.getId().equals(cause.getId())
                    && failureCause.getCategories().equals(cause.getCategories())) {
                return true;
            }
        }
        return false;
    }
}
