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

package com.sonyericsson.jenkins.plugins.bfa.model;

import org.htmlunit.WebClientUtil;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseListener;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTextArea;
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link FailureCause}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@WithJenkins
class FailureCauseHudsonTest {

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)}.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoConfigSubmit(JenkinsRule jenkins) throws Exception {
        List<FailureCause> list = new LinkedList<>();
        FailureCause c = new FailureCause("A Name", "Some Description");
        String id = "uniqueID";
        c.setId(id);
        c.addIndication(new BuildLogIndication("some pattern"));
        list.add(c);
        c = new FailureCause("An other Name", "Some other Description");
        c.setId("anotherID");
        c.addIndication(new BuildLogIndication("some other pattern"));
        list.add(c);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(list));

        JenkinsRule.WebClient client = jenkins.createWebClient();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/" + id);

        jenkins.submit(page.getFormByName("causeForm"));

        Collection<FailureCause> newList = PluginImpl.getInstance().getKnowledgeBase().getCauses();

        assertEquals(list.size(), newList.size());
        Iterator<FailureCause> iterator = newList.iterator();

        FailureCause cause1 = list.get(0);
        FailureCause cause2 = list.get(1);
        //Is it ok that they end up in different order than inserted?
        while (iterator.hasNext()) {
            FailureCause oldCause = null;
            FailureCause next = iterator.next();
            if (next.getName().equals(cause1.getName())) {
                oldCause = cause1;
            } else if (next.getName().equals(cause2.getName())) {
                oldCause = cause2;
            } else {
                fail("Unexpected cause saved: " + next.getName());
            }
            assertNotNull(next.getId(), "It should have an id!");
            assertEquals(oldCause.getName(), next.getName());
            assertEquals(oldCause.getDescription(), next.getDescription());
            assertEquals(oldCause.getIndications().get(0).getPattern().pattern(),
                    next.getIndications().get(0).getPattern().pattern());
        }
    }

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)} with only one cause configured.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoConfigSubmitOne(JenkinsRule jenkins) throws Exception {
        List<FailureCause> list = new LinkedList<>();
        FailureCause c = new FailureCause("A Name", "Some Description");
        String id = "uniqueID";
        c.setId(id);
        c.addIndication(new BuildLogIndication("some pattern"));
        list.add(c);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(list));

        JenkinsRule.WebClient client = jenkins.createWebClient();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/" + id);

        jenkins.submit(page.getFormByName("causeForm"));

        Collection<FailureCause> newList = PluginImpl.getInstance().getKnowledgeBase().getCauses();

        assertEquals(list.size(), newList.size());
        FailureCause cause = list.get(0);
        FailureCause newCause = newList.iterator().next();
        assertEquals(cause.getName(), newCause.getName());
        assertEquals(cause.getDescription(), newCause.getDescription());
        assertEquals(cause.getIndications().get(0).getPattern().pattern(),
                newCause.getIndications().get(0).getPattern().pattern());
    }

    /**
     * Test for JENKINS-47027. Checks that the create validation request does not return an 500.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoCheckNameViaWebForm(JenkinsRule jenkins) throws Exception {
        JenkinsRule.WebClient client = jenkins.createWebClient();

        WebResponseListener.StatusListener serverErrors =
                new WebResponseListener.StatusListener(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        WebResponseListener.StatusListener success = new WebResponseListener.StatusListener(HttpStatus.SC_OK);

        client.addWebResponseListener(serverErrors);
        client.addWebResponseListener(success);

        client.loadDownloadedResponses();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/new");

        HtmlInput input = page.getFormByName("causeForm").getInputByName("_.name");
        input.setValue("Mööp");
        input.fireEvent("change");
        WebClientUtil.waitForJSExec(page.getWebClient());

        assertTrue(serverErrors.getResponses().isEmpty());
        WebResponse webResponse = success.getResponses().get(success.getResponses().size() - 1);

        assertTrue(webResponse.getWebRequest().getUrl().getPath().endsWith(
                "/descriptorByName/com.sonyericsson.jenkins.plugins.bfa.model.FailureCause/checkName"));
        assertEquals("<div/>", webResponse.getContentAsString());
    }

    /**
     * Test for JENKINS-47027. Checks that the create validation request does not return an 500.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testDoCheckDescriptionViaWebForm(JenkinsRule jenkins) throws Exception {
        JenkinsRule.WebClient client = jenkins.createWebClient();

        WebResponseListener.StatusListener serverErrors =
                new WebResponseListener.StatusListener(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        WebResponseListener.StatusListener success = new WebResponseListener.StatusListener(HttpStatus.SC_OK);

        client.addWebResponseListener(serverErrors);
        client.addWebResponseListener(success);

        client.loadDownloadedResponses();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/new");

        HtmlTextArea input = page.getFormByName("causeForm").getTextAreaByName("_.description");
        input.setText("Mööp");
        input.fireEvent("change");
        WebClientUtil.waitForJSExec(page.getWebClient());

        assertTrue(serverErrors.getResponses().isEmpty());
        WebResponse webResponse = success.getResponses().get(success.getResponses().size() - 1);

        assertTrue(webResponse.getWebRequest().getUrl().getPath().endsWith(
                "/descriptorByName/com.sonyericsson.jenkins.plugins.bfa.model.FailureCause/checkDescription"));
        assertEquals("<div/>", webResponse.getContentAsString());
    }
}
