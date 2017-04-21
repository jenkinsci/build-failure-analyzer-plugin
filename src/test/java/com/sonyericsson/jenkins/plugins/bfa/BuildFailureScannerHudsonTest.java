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

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.listeners.RunListener;
import hudson.tasks.junit.JUnitResultArchiver;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//CS IGNORE MagicNumber FOR NEXT 1000 LINES. REASON: TestData.

/**
 * Tests for the FailureScanner.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */

public class BuildFailureScannerHudsonTest {

    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule jenkins = new JenkinsRule();

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
     * @throws Exception if so.
     */
    @Test
    public void testOneIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        FailureCause failureCause = configureCauseAndIndication();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

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
        HtmlElement focus = document.getElementById(id);
        assertNotNull(focus);

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", foundFailureCause.getName());
        assertNotNull(errorElements);
        HtmlElement error = errorElements.get(0);

        assertNotNull(error);
        assertEquals("Error message not found: ", BUILD_LOG_FIRST_LINE, error.getTextContent().trim());
    }

    /**
     * Happy test that should find one failure indication in the build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testOneMultilineIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        FailureCause failureCause = configureCauseAndIndication(new MultilineBuildLogIndication(MULTILINE_REGEX));

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

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
        HtmlElement focus = document.getElementById(id);
        assertNotNull(focus);

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", foundFailureCause.getName());
        assertNotNull(errorElements);
        HtmlElement error = errorElements.get(0);

        assertNotNull(error);
        assertEquals("Error message not found: ",
                new StringTokenizer(BUILD_LOG).nextToken("\n"),
                error.getTextContent().trim());
    }

    /**
     * Happy test that should find two failure causes in the build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testTwoIndicationsSameLine() throws Exception {
        FreeStyleProject project = createProject();

        FailureCause failureCause = configureCauseAndIndication();

        Indication indication = new BuildLogIndication(REGEX);
        final String otherDescription = "Other description";
        FailureCause failureCause2 = configureCauseAndIndication("Other cause", otherDescription, indication);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));
        assertTrue(findCauseInList(causeListFromAction, failureCause2));

        HtmlPage page = jenkins.createWebClient().goTo(build.getUrl() + "console");

        HtmlElement document = page.getDocumentElement();

        final Set<String> causeDescriptions = new HashSet<String>();
        causeDescriptions.add(FORMATTED_DESCRIPTION);
        causeDescriptions.add(otherDescription);

        FoundFailureCause foundFailureCause = causeListFromAction.get(0);
        String description = foundFailureCause.getDescription();
        assertTrue(causeDescriptions.remove(description));
        FoundIndication foundIndication = foundFailureCause.getIndications().get(0);
        String id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        HtmlElement focus = document.getElementById(id);
        assertNotNull(focus);

        foundFailureCause = causeListFromAction.get(1);
        description = foundFailureCause.getDescription();
        assertTrue(causeDescriptions.remove(description));
        foundIndication = foundFailureCause.getIndications().get(0);
        id = foundIndication.getMatchingHash() + foundFailureCause.getId();
        focus = document.getElementById(id);
        assertNotNull(focus);
        assertTrue(causeDescriptions.isEmpty());

        String title = failureCause.getName() + "\n" + failureCause2.getName();

        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "title", title);
        //The titles could be in any given order, trying both orders before failing.
        if (errorElements.size() < 1) {
            title = failureCause2.getName() + "\n" + failureCause.getName();
            errorElements = document.getElementsByAttribute("span", "title", title);
        }
        assertTrue("Title not found in annotated text", errorElements.size() > 0);
        HtmlElement error = errorElements.get(0);
        assertNotNull(error);
        assertEquals("Error message not found: ", BUILD_LOG_FIRST_LINE, error.getTextContent().trim());
    }

    /**
     * One indication should be found and a correct looking Gerrit-Trigger-Plugin message can be constructed.
     *
     * @throws Exception if so.
     */
    @Test
    public void testOneIndicationBuildCompletedMessage() throws Exception {
        PluginImpl.getInstance().setGerritTriggerEnabled(true);

        FreeStyleProject project = createProject();

        configureCauseAndIndication();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);

        GerritMessageProviderExtension messageProvider = new GerritMessageProviderExtension();

        assertEquals("The " + GerritMessageProviderExtension.class.getSimpleName()
                + " extension would not return the expected message.",
                FORMATTED_DESCRIPTION + " ( " + Jenkins.getInstance().getRootUrl() + "job/test0/1/ )",
                messageProvider.getBuildCompletedMessage(build));

        PluginImpl.getInstance().setGerritTriggerEnabled(false);

        assertNull("The " + GerritMessageProviderExtension.class.getSimpleName()
                + " extension would not return null.",
                messageProvider.getBuildCompletedMessage(build));
    }

    /**
     * Unhappy test that should not find any failure indications in the build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testNoIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        configureCauseAndIndication(new BuildLogIndication(".*something completely different.*"));

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        assertTrue(action.getFoundFailureCauses().isEmpty());
    }

    /**
     * Unhappy test that should not find any failure multiline indications in the build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testNoMultilineIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        configureCauseAndIndication(new MultilineBuildLogIndication(".*something completely different.*"));

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        assertTrue(action.getFoundFailureCauses().isEmpty());
    }

    /**
     * Makes sure that the build action is not added to a successful build.
     *
     * @throws Exception if so.
     */
    @Test
    public void testSuccessfulBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(BUILD_LOG));
        configureCauseAndIndication();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when the the global setting {@link PluginImpl#isGlobalEnabled()} is false.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoNotScanGlobal() throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(false);
        FreeStyleProject project = createProject();
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when build log size exceeds max log size.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoNotScanIfLogSizeExceedsLimit() throws Exception {
        PluginImpl.getInstance().setMaxLogSize(1);
        FreeStyleProject project = createProject(createHugeString(1024 * 1024) + BUILD_LOG);
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that scanner result presents when build log size is less than max log size.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoScanIfLogSizeIsInLimit() throws Exception {
        PluginImpl.getInstance().setMaxLogSize(2);
        FreeStyleProject project = createProject(createHugeString(1024 * 1024) + BUILD_LOG);
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
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
        for (int i = 0; i < length; i++) {
            text[i] = 'a';
        }
        return new String(text);
    }

    /**
     * Tests that there is no scanner result when the property
     * {@link com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty}
     * is set to true.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoNotScanSpecific() throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(true);
        FreeStyleProject project = createProject();
        project.addProperty(new ScannerJobProperty(true));
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }


    /**
     * Tests that the saveStatistics method of KnowledgeBase is called with a Statistics object.
     *
     * @throws Exception if so.
     */
    @Test
    public void testStatisticsLogging() throws Exception {

        Indication indication = new BuildLogIndication(REGEX);
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(indication);
        FailureCause cause = new FailureCause("myId", "testcause", "testdescription", "testcomment",
                                              null, "testcategory", indicationList, null);
        List<FailureCause> causes = new LinkedList<FailureCause>();
        causes.add(cause);
        KnowledgeBase base = mock(KnowledgeBase.class);
        when(base.isStatisticsEnabled()).thenReturn(true);
        when(base.getCauses()).thenReturn(causes);
        when(base.isStatisticsEnabled()).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                hasCalledStatistics = true;
                return null;
            }
        }).when(base).saveStatistics(Matchers.<Statistics>any());
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, base);
        FreeStyleProject project = createProject();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
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
     */
    @Test
    public void testOrdinal() {
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
        assertTrue("BFA (" + bfaPlacement + ") should list before GT (" + gtPlacement + ")", bfaPlacement < gtPlacement);
    }

    /**
     * Test whether failed test cases are successfully matched as failure causes.
     *
     * @throws Exception if not so.
     */
    @Test
    public void testTestResultInterpretation() throws Exception {
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

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);

        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals("Amount of failure causes does not match.", 2, causeListFromAction.size());

        assertEquals(causeListFromAction.get(0).getName(), "AFailingTest");
        assertEquals(causeListFromAction.get(0).getDescription(), "Here are details of the failure...");
        assertEquals(new ArrayList<String>(), causeListFromAction.get(0).getCategories());
        assertEquals(causeListFromAction.get(1).getName(), "AnotherFailingTest");
        assertEquals(causeListFromAction.get(1).getDescription(), "More details");
        assertEquals(new ArrayList<String>(), causeListFromAction.get(1).getCategories());
    }

    /**
     * Test whether failed test cases are registered against the configured categories.
     *
     * @throws Exception if not so.
     */
    @Test
    public void testTestResultInterpretationWithCategories() throws Exception {
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

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);

        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals("Amount of failure causes does not match.", 2, causeListFromAction.size());

        List<String> categoriesList = Arrays.asList(categories.split("\\s+"));
        assertEquals(categoriesList, causeListFromAction.get(0).getCategories());
        assertEquals(categoriesList, causeListFromAction.get(1).getCategories());
    }

    /**
     * Test whether failed test cases are not detected when feature is disabled.
     *
     * @throws Exception if not so.
     */
    @Test
    public void testTestResultInterpretationIfDisabled() throws Exception {
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

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        jenkins.assertBuildStatus(Result.UNSTABLE, build);

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertEquals("Amount of failure causes does not match.", 0, causeListFromAction.size());
    }

    /**
     * ArgumentMatcher for a Statistics object.
     */
    public static class IsValidStatisticsObject extends ArgumentMatcher<Statistics> {
        @Override
        public boolean matches(Object o) {
            if (!(o instanceof Statistics)) {
                return false;
            }
            Statistics stat = (Statistics)o;
            if (stat.getBuildNumber() != 1) {
                return false;
            }
            List<FailureCauseStatistics> failureCauseStatisticsList = stat.getFailureCauseStatisticsList();
            if (failureCauseStatisticsList == null || failureCauseStatisticsList.size() != 1) {
                return false;
            }
            if (!"myId".equals(failureCauseStatisticsList.get(0).getId())) {
                return false;
            }
            return true;
        }
    }

    //CS IGNORE LineLength FOR NEXT 11 LINES. REASON: JavaDoc.

    /**
     * Convenience method for a standard cause that finds {@link #BUILD_LOG} in the build log.
     *
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     * @see #configureCauseAndIndication(String, String, com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication() throws Exception {
        return configureCauseAndIndication(new BuildLogIndication(REGEX));
    }

    //CS IGNORE LineLength FOR NEXT 10 LINES. REASON: JavaDoc.

    /**
     * Convenience method for the standard cause with a special indication.
     *
     * @param indication the indication for the cause.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(String, String, com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication(Indication indication) throws Exception {
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
    private FailureCause configureCauseAndIndication(String name, String description, Indication indication)
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
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(indication);
        FailureCause failureCause =
                new FailureCause(name, name, description, comment, null, category, indicationList, null);

        Collection<FailureCause> causes = PluginImpl.getInstance().getKnowledgeBase().getCauses();

        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.addAll(causes);
        causeList.add(failureCause);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(causeList));
        return failureCause;
    }

    /**
     * Creates a project that prints {@link #BUILD_LOG} to the console and fails the build.
     *
     * @return the project
     *
     * @throws IOException if so.
     */
    private FreeStyleProject createProject() throws IOException {
        return createProject(BUILD_LOG);
    }

    /**
     * Creates a project that prints the given log string to the console and fails the build.
     *
     * @param logString the string to appear in the build log
     * @return the project
     * @throws IOException if so
     */
    private FreeStyleProject createProject(String logString) throws IOException {
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
