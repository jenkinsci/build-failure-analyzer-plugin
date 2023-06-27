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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.sonyericsson.jenkins.plugins.bfa.test.utils.JenkinsRuleWithMatrixSupport;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.recipes.LocalData;

import org.htmlunit.html.HtmlPage;

/**
 * Tests for {@link FailureCauseColumn}.
 *
 * @author Vincent Latombe &lt;vincent@latombe.net&gt;
 */
public class FailureCauseColumnTest {

  /**
   * The Jenkins Rule.
   */
  @Rule
  //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
  public JenkinsRuleWithMatrixSupport j = new JenkinsRuleWithMatrixSupport();

  /**
   * Happy test case with a view containing a {@link FailureCauseColumn}, text option being disabled.
   *
   * @throws Exception
   *           if so
   */
  @LocalData
  @Test
  public void givenAViewWithTheFailureCauseColumnDisplayTheFirstFailureCauseAsTitle() throws Exception {
    FreeStyleProject fs = j.createFreeStyleProject("total_failure");
    fs.getBuildersList().add(new FailureBuilder());
    fs.save();

    FreeStyleBuild r = fs.scheduleBuild2(0).get();
    j.assertBuildStatus(Result.FAILURE, r);

    WebClient webClient = j.createWebClient();
    HtmlPage page = webClient.goTo("view/columnwithouttext");
    assertNotNull("Couldn't find the failure cause image in columnwithouttext view",
            page.getFirstByXPath("//img[@title='Failure Builder']"));
    assertNull(page.getDocumentElement().getFirstByXPath("//*[.='Failure Builder']"));
  }

  /**
   * Happy test case with a view containing a {@link FailureCauseColumn}, text option being enabled.
   *
   * @throws Exception
   *           if so
   */
  @LocalData
  @Test
  public void givenAViewWithTheFailureCauseColumnWithTextDisplayTheFirstFailureCauseAsTitleAndText() throws Exception {
    FreeStyleProject fs = j.createFreeStyleProject("total_failure");
    fs.getBuildersList().add(new FailureBuilder());
    fs.save();

    FreeStyleBuild r = fs.scheduleBuild2(0).get();
    j.assertBuildStatus(Result.FAILURE, r);

    WebClient webClient = j.createWebClient();
    HtmlPage page = webClient.goTo("view/columnwithtext");
    System.out.println(page.getTextContent());
    assertNotNull("Couldn't find the failure cause image in columnwithtext view",
        page.getFirstByXPath("//img[@title='Failure Builder']"));
    assertNotNull(page.getFirstByXPath("//*[.='Failure Builder']"));
  }

}
