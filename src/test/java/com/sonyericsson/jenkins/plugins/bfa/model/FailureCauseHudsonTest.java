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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests for {@link FailureCause}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class FailureCauseHudsonTest extends HudsonTestCase {

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    public void testDoConfigSubmit() throws Exception {
        List<FailureCause> list = new LinkedList<FailureCause>();
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

        WebClient client = this.createWebClient();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/" + id);

        this.submit(page.getFormByName("causeForm"));

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
            assertNotNull("It should have an id!", next.getId());
            assertEquals(oldCause.getName(), next.getName());
            assertEquals(oldCause.getDescription(), next.getDescription());
            assertEquals(oldCause.getIndications().get(0).getPattern().toString(),
                    next.getIndications().get(0).getPattern().toString());
        }
    }

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)} with only one cause configured.
     *
     * @throws Exception if so.
     */
    public void testDoConfigSubmitOne() throws Exception {
        List<FailureCause> list = new LinkedList<FailureCause>();
        FailureCause c = new FailureCause("A Name", "Some Description");
        String id = "uniqueID";
        c.setId(id);
        c.addIndication(new BuildLogIndication("some pattern"));
        list.add(c);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(list));

        WebClient client = this.createWebClient();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME + "/" + id);

        this.submit(page.getFormByName("causeForm"));

        Collection<FailureCause> newList = PluginImpl.getInstance().getKnowledgeBase().getCauses();

        assertEquals(list.size(), newList.size());
        FailureCause cause = list.get(0);
        FailureCause newCause = newList.iterator().next();
        assertEquals(cause.getName(), newCause.getName());
        assertEquals(cause.getDescription(), newCause.getDescription());
        assertEquals(cause.getIndications().get(0).getPattern().toString(),
                newCause.getIndications().get(0).getPattern().toString());
    }
}
