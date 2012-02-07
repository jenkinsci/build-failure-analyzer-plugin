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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.LinkedList;
import java.util.List;

/**
 * Hudson tests for {@link CauseManagement}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class CauseManagementTest extends HudsonTestCase {

    /**
     * Happy test for {@link CauseManagement#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    public void testDoConfigSubmit() throws Exception {
        List<FailureCause> list = new LinkedList<FailureCause>();
        FailureCause c = new FailureCause("A Name", "Some Description");
        c.addIndicator(new BuildLogIndication("some pattern"));
        list.add(c);
        c = new FailureCause("An other Name", "Some other Description");
        c.addIndicator(new BuildLogIndication("some other pattern"));
        list.add(c);
        PluginImpl.getInstance().setCauses(list);

        WebClient client = this.createWebClient();
        HtmlPage page = client.goTo(CauseManagement.URL_NAME);

        this.submit(page.getFormByName("causesForm"));

        List<FailureCause> newList = PluginImpl.getInstance().getCauses().getView();

        assertEquals(list.size(), newList.size());
        FailureCause cause = list.get(0);
        FailureCause newCause = newList.get(0);
        assertEquals(cause.getName(), newCause.getName());
        assertEquals(cause.getDescription(), newCause.getDescription());
        assertEquals(cause.getIndications().get(0).getPattern().pattern(),
                newCause.getIndications().get(0).getPattern().pattern());
        assertNotSame(cause, newCause);

        cause = list.get(1);
        newCause = newList.get(1);
        assertEquals(cause.getName(), newCause.getName());
        assertEquals(cause.getDescription(), newCause.getDescription());
        assertEquals(cause.getIndications().get(0).getPattern().pattern(),
                newCause.getIndications().get(0).getPattern().pattern());
        assertNotSame(cause, newCause);
    }

    /**
     * Tests {@link com.sonyericsson.jenkins.plugins.bfa.CauseManagement#isUnderTest()}.
     */
    public void testIsUnderTest() {
        assertTrue(CauseManagement.getInstance().isUnderTest());
    }
}
