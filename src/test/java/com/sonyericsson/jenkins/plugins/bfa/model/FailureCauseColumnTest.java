/*
 * The MIT License
 *
 * Copyright 2014 Vincent Latombe
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
package com.sonyericsson.jenkins.plugins.bfa.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import org.htmlunit.html.HtmlPage;

/**
 * Tests for {@link FailureCauseColumn}.
 *
 * @author Vincent Latombe &lt;vincent@latombe.net&gt;
 */
@WithJenkins
class FailureCauseColumnTest {

    /**
     * Happy test case with a view containing a {@link FailureCauseColumn}, text option being disabled.
     *
     * @param j
     *
     * @throws Exception
   *           if so
     */
    @LocalData
    @Test
    void givenAViewWithTheFailureCauseColumnDisplayTheFirstFailureCauseAsTitle(JenkinsRule j) throws Exception {
    FreeStyleProject fs = j.createFreeStyleProject("total_failure");
    fs.getBuildersList().add(new FailureBuilder());
    fs.save();

    FreeStyleBuild r = fs.scheduleBuild2(0).get();
    j.assertBuildStatus(Result.FAILURE, r);

    WebClient webClient = j.createWebClient();
    HtmlPage page = webClient.goTo("view/columnwithouttext");
    assertNotNull(page.getFirstByXPath("//*[local-name()='svg'][@tooltip='Failure Builder']"),
                  "Couldn't find the failure cause svg in columnwithouttext view");
    assertNull(page.getDocumentElement().getFirstByXPath("//*[.='Failure Builder']"));
  }

    /**
     * Happy test case with a view containing a {@link FailureCauseColumn}, text option being enabled.
     *
     * @param j
     *
     * @throws Exception
     *        if so
     */
    @LocalData
    @Test
    void givenAViewWithTheFailureCauseColumnWithTextDisplayTheFirstFailureCauseAsTitleAndText(JenkinsRule j)
            throws Exception {
    FreeStyleProject fs = j.createFreeStyleProject("total_failure");
    fs.getBuildersList().add(new FailureBuilder());
    fs.save();

    FreeStyleBuild r = fs.scheduleBuild2(0).get();
    j.assertBuildStatus(Result.FAILURE, r);

    WebClient webClient = j.createWebClient();
    HtmlPage page = webClient.goTo("view/columnwithtext");
    System.out.println(page.getTextContent());
    assertNotNull(page.getFirstByXPath("//*[local-name()='svg'][@tooltip='Failure Builder']"),
                  "Couldn't find the failure cause svg in columnwithtext view");
    assertNotNull(page.getFirstByXPath("//*[contains(text(), 'Failure Builder')]"));
  }

}
