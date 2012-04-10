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

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import hudson.Util;
import hudson.util.Secret;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.Iterator;

/**
 * Hudson tests for {@link CauseManagement}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class CauseManagementHudsonTest extends HudsonTestCase {

    /**
     * Tests {@link com.sonyericsson.jenkins.plugins.bfa.CauseManagement#isUnderTest()}.
     */
    public void testIsUnderTest() {
        assertTrue(CauseManagement.getInstance().isUnderTest());
    }

    /**
     * Verifies that the table on the {@link CauseManagement} page displays all causes with description and that
     * one of them can be navigated to and a valid edit page for that cause is shown.
     *
     * @throws Exception if so.
     */
    public void testTableViewNavigation() throws Exception {
        FailureCause cause = new FailureCause("SomeName", "A Description");
        cause.addIndication(new BuildLogIndication("."));
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause);
        cause = new FailureCause("SomeOtherName", "A Description");
        cause.addIndication(new BuildLogIndication("."));
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause);

        WebClient web = createWebClient();
        HtmlPage page = web.goTo(CauseManagement.URL_NAME);
        HtmlTable table = (HtmlTable)page.getElementById("failureCausesTable");

        Collection<FailureCause> expectedCauses = PluginImpl.getInstance().getKnowledgeBase().getShallowCauses();

        int rowCount = table.getRowCount();
        assertEquals(expectedCauses.size() + 1, rowCount);
        Iterator<FailureCause> causeIterator = expectedCauses.iterator();

        FailureCause firstCause = null;

        for (int i = 1; i < rowCount; i++) {
            assertTrue(causeIterator.hasNext());
            FailureCause c = causeIterator.next();
            HtmlTableRow row = table.getRow(i);
            String name = row.getCell(0).getTextContent();
            String description = row.getCell(1).getTextContent();
            assertEquals(c.getName(), name);
            assertEquals(c.getDescription(), description);
            if (i == 1) {
                firstCause = c;
            }
        }

        //The table looks ok, now lets see if we can navigate correctly.

        assertNotNull(firstCause);

        HtmlAnchor firstCauseLink = (HtmlAnchor)table.getCellAt(1, 0).getFirstChild();
        HtmlPage editPage = firstCauseLink.click();

        verifyCorrectCauseEditPage(firstCause, editPage);
    }

    /**
     * Tests that the "new cause" link on the page navigates to a correct page.
     * @throws Exception if so.
     */
    public void testNewNavigation() throws Exception {

        WebClient web = createWebClient();
        HtmlPage page = web.goTo(CauseManagement.URL_NAME);

        HtmlAnchor newLink = page.getAnchorByHref(CauseManagement.NEW_CAUSE_DYNAMIC_ID);
        HtmlPage editPage = newLink.click();

        verifyCorrectCauseEditPage(new FailureCause(
                CauseManagement.NEW_CAUSE_NAME,
                CauseManagement.NEW_CAUSE_DESCRIPTION),
                editPage);
    }
    //CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.
    /**
     * Tests that an error message is shown when there is no reachable Mongo database.
     * @throws Exception if so.
     */
    public void testNoMongoDB() throws Exception {
        KnowledgeBase kb = new MongoDBKnowledgeBase("someurl", 1234, "somedb", "user", Secret.fromString("pass"));
        Whitebox.setInternalState(PluginImpl.getInstance(), kb);
        WebClient web = createWebClient();
        HtmlPage page = web.goTo(CauseManagement.URL_NAME);
        HtmlElement element =  page.getElementById("errorMessage");
        assertNotNull(element);
    }

    /**
     * Verifies that the page is displaying the expected cause correctly.
     *
     * @param expectedCause the cause that is expected to be displayed.
     * @param editPage      the page to verify.
     * @see #testNewNavigation()
     * @see #testTableViewNavigation()
     */
    private void verifyCorrectCauseEditPage(FailureCause expectedCause, HtmlPage editPage) {
        HtmlForm form = editPage.getFormByName("causeForm");
        String actualId = form.getInputByName("_.id").getValueAttribute();
        if (Util.fixEmpty(expectedCause.getId()) == null) {
            assertNull(Util.fixEmpty(actualId));
        } else {
            assertEquals(expectedCause.getId(), actualId);
        }
        assertEquals(expectedCause.getName(), form.getInputByName("_.name").getValueAttribute());
        HtmlElement descrArea = form.getOneHtmlElementByAttribute("textarea", "name", "_.description");
        String description = descrArea.getTextContent();
        assertEquals(expectedCause.getDescription(), description);

        if (!expectedCause.getIndications().isEmpty()) {
            HtmlElement indicationsDiv = form.getOneHtmlElementByAttribute("div", "name", "indications");
            HtmlInput patternInput = indicationsDiv.getOneHtmlElementByAttribute("input", "name", "_.pattern");
            assertEquals(expectedCause.getIndications().get(0).getPattern().pattern(), patternInput.getValueAttribute());
        }
    }
}
