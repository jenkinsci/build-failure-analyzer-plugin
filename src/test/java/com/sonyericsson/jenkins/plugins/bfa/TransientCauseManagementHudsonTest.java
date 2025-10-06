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

import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Shell;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

//CS IGNORE MagicNumber FOR NEXT 300 LINES. REASON: TestData.

/**
 * Tests for the config page being transient.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@WithJenkins
class TransientCauseManagementHudsonTest {

    /**
     * Tests that the {@link CauseManagement} link is present on a freestyle project and that you can navigate to it.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOnAProject(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("nils-job");
        project.getBuildersList().add(new Shell("env | sort"));
        project = (FreeStyleProject)jenkins.configRoundtrip((Item)project);
        JenkinsRule.WebClient web = jenkins.createWebClient();
        HtmlPage page = web.goTo(project.getUrl());

        HtmlAnchor anchor = getAnchorBySuffix(page, CauseManagement.URL_NAME);
        HtmlPage configPage = anchor.click();
        assertDoesNotThrow(() -> verifyIsConfigurationPage(configPage), "The link should be there!");
    }

    /**
     * Tests that the {@link CauseManagement} link is present on a freestyle build and that you can navigate to it.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testOnABuild(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("nils-job");
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get(10, TimeUnit.SECONDS);
        JenkinsRule.WebClient web = jenkins.createWebClient();
        HtmlPage page = web.goTo(build.getUrl());

        HtmlAnchor anchor = getAnchorBySuffix(page, FailureCauseBuildAction.URL_NAME);
        HtmlPage configPage = anchor.click();
        assertDoesNotThrow(() -> verifyIsConfigurationPage(configPage), "The link should be there!");
    }

    /**
     * Finds an anchor on the page who's href attribute ends with the provided suffix.
     *
     * @param page   the page
     * @param suffix the suffix
     * @return the anchor
     *
     * @throws ElementNotFoundException if no anchor was found.
     */
    private static HtmlAnchor getAnchorBySuffix(HtmlPage page, String suffix) {
        List<HtmlAnchor> anchors = page.getAnchors();
        for (HtmlAnchor anchor : anchors) {
            if (anchor.getHrefAttribute().endsWith(suffix)) {
                return anchor;
            }
        }
        throw new ElementNotFoundException("a", "href", ".*" + suffix);
    }

    /**
     * Verifies that the provided page is the cause management page, by doing some checks on specific elements.
     *
     * @param page the page to verify.
     * @throws ElementNotFoundException if so.
     */
    private static void verifyIsConfigurationPage(HtmlPage page) throws ElementNotFoundException {
        //Some smoke test to see if it is the correct page
        HtmlAnchor newAnchor = getAnchorBySuffix(page, "new");
        assertThat("New Cause link has wrong text!",
                    newAnchor.getTextContent(), containsString("Create new"));
        DomNodeList<DomElement> elementsByTagName = page.getElementsByTagName("h1");
        boolean headingFound = false;
        for (DomElement element : elementsByTagName) {
            if ("Update Failure Causes".equals(element.getTextContent())) {
                headingFound = true;
                break;
            }
        }
        assertTrue(headingFound, "H1 Heading not found!");
    }
}

