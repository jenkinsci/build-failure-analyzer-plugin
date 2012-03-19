/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerOffJobProperty;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//CS IGNORE MagicNumber FOR NEXT 300 LINES. REASON: TestData.

/**
 * Tests for the FailureScanner.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */

public class FailureScannerTest extends HudsonTestCase {

    private static final String TO_PRINT = "ERROR";

    /**
     * Happy test that should find one failure indication in the build.
     *
     * @throws Exception if so.
     */
    public void testOneIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        FailureCause failureCause = configureCauseAndIndication();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));

        WebClient web = createWebClient();
        HtmlPage page = web.goTo("/" + build.getUrl() + action.getUrlName() + "/1/1");
        HtmlElement document = page.getDocumentElement();
        HtmlElement focus = document.getElementById("focusLine");
        List<HtmlElement> errorElements = document.getElementsByAttribute("span", "class", "errorLine");
        assertNotNull(errorElements);
        HtmlElement error = errorElements.get(0);
        assertNotNull(focus);
        assertNotNull(error);
        assertEquals("Error message not found: ", error.getTextContent(), TO_PRINT);
    }

    /**
     * Unhappy test that should not find any failure indications in the build.
     *
     * @throws Exception if so.
     */
    public void testNoIndicationFound() throws Exception {
        FreeStyleProject project = createProject();

        FailureCause failureCause = configureCauseAndIndication(
                new BuildLogIndication(".*something completely different.*"));

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FoundFailureCause> causeListFromAction = action.getFoundFailureCauses();
        assertTrue(causeListFromAction.size() == 0);
    }

    /**
     * Makes sure that the build action is not added to a successful build.
     *
     * @throws Exception if so.
     */
    public void testSuccessfulBuild() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        configureCauseAndIndication();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when the the global setting {@link PluginImpl#isGlobalEnabled()} is false.
     *
     * @throws Exception if so.
     */
    public void testDoNotScanGlobal() throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(false);
        FreeStyleProject project = createProject();
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Tests that there is no scanner result when the property {@link ScannerOffJobProperty} is set to true.
     *
     * @throws Exception if so.
     */
    public void testDoNotScanSpecific() throws Exception {
        PluginImpl.getInstance().setGlobalEnabled(true);
        FreeStyleProject project = createProject();
        project.addProperty(new ScannerOffJobProperty(true));
        configureCauseAndIndication();
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());

        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    //CS IGNORE LineLength FOR NEXT 11 LINES. REASON: JavaDoc.

    /**
     * Convenience method for a standard cause that finds {@link #TO_PRINT} in the build log.
     *
     * @return the configured cause that was added to the global config.
     *
     * @see #configureCauseAndIndication(com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     * @see #configureCauseAndIndication(String, String, com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication() {
        return configureCauseAndIndication(new BuildLogIndication(".*ERROR.*"));
    }

    //CS IGNORE LineLength FOR NEXT 10 LINES. REASON: JavaDoc.

    /**
     * Convenience method for the standard cause with a special indication.
     *
     * @param indication the indication for the cause.
     * @return the configured cause that was added to the global config.
     *
     * @see #configureCauseAndIndication(String, String, com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication(Indication indication) {
        return configureCauseAndIndication("Error", "This is an error", indication);
    }

    /**
     * Configures the global settings with a cause that has the provided indication/
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     */
    private FailureCause configureCauseAndIndication(String name, String description, Indication indication) {
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(indication);
        FailureCause failureCause = new FailureCause(name, name, description, indicationList);
        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.add(failureCause);
        PluginImpl.getInstance().setCauses(causeList);
        return failureCause;
    }

    /**
     * Creates a project that prints {@link #TO_PRINT} to the console and fails the build.
     *
     * @return the project
     *
     * @throws IOException if so.
     */
    private FreeStyleProject createProject() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
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
    private boolean findCauseInList(List<FoundFailureCause> causeListFromAction, FailureCause failureCause) {
        for (FoundFailureCause cause : causeListFromAction) {
            if (failureCause.getName().equals(cause.getName())
                && failureCause.getDescription().equals(cause.getDescription())) {
                return true;
            }
        }
        return false;
    }

}
