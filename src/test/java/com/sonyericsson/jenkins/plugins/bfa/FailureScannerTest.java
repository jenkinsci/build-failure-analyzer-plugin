/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;
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
     * @throws Exception if so.
     */
    public void testOneIndicationFound() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));

        Indication ind = new BuildLogIndication(".*ERROR.*");
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(ind);
        FailureCause failureCause = new FailureCause("Error", "This is an error", indicationList);
        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.add(failureCause);
        PluginImpl.getInstance().setCauses(causeList);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());

        FreeStyleBuild build =  future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FailureCause> causeListFromAction = action.getFailureCauses();
        assertTrue(findCauseInList(causeListFromAction, failureCause));
    }

    /**
     * Unhappy test that should not find any failure indications in the build.
     * @throws Exception if so.
     */
    public void testNoIndicationFound() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));

        Indication ind = new BuildLogIndication(".*something completely different.*");
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(ind);
        FailureCause failureCause = new FailureCause("Error", "This is an error", indicationList);
        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.add(failureCause);
        PluginImpl.getInstance().setCauses(causeList);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());
        FreeStyleBuild build =  future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNotNull(action);
        List<FailureCause> causeListFromAction = action.getFailureCauses();
        assertTrue(causeListFromAction.size() == 0);
    }

    /**
     * Makes sure that the build action is not added to a successful build.
     * @throws Exception if so.
     */
    public void testSuccessfulBuild() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(TO_PRINT));
        Indication ind = new BuildLogIndication(".*ERROR.*");
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(ind);
        FailureCause failureCause = new FailureCause("Error", "This is an error", indicationList);
        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.add(failureCause);
        PluginImpl.getInstance().setCauses(causeList);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());
        FreeStyleBuild build =  future.get(10, TimeUnit.SECONDS);
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        assertNull(action);
    }

    /**
     * Searches the list for the FailureCause.
     * @param causeListFromAction the list.
     * @param failureCause the cause.
     * @return true if found, false if not.
     */
    private boolean findCauseInList(List<FailureCause> causeListFromAction, FailureCause failureCause) {
        for (FailureCause cause : causeListFromAction) {
            if (failureCause.equals(cause)) {
                return true;
            }
        }
        return false;
    }

}
