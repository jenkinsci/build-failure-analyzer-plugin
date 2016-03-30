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

package com.sonyericsson.jenkins.plugins.bfa;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.DifferentKnowledgeBase;
import hudson.ExtensionList;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.powermock.reflect.Whitebox;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link HudsonTestCase}s for {@link PluginImpl}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImplHudsonTest { //extends HudsonTestCase {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    /**
     * Tests that {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getKnowledgeBaseDescriptors()} contains the
     * default descriptors.
     *
     * @throws Exception if so.
     */
    @Test
    public void testGetKnowledgeBaseDescriptors() throws Exception {
        ExtensionList<KnowledgeBase.KnowledgeBaseDescriptor> descriptors =
                PluginImpl.getInstance().getKnowledgeBaseDescriptors();
        boolean foundLocalFile = false;
        boolean foundMongoDB = false;
        boolean foundDifferentKb = false;
        for (KnowledgeBase.KnowledgeBaseDescriptor descriptor : descriptors) {
            if (descriptor instanceof LocalFileKnowledgeBase.LocalFileKnowledgeBaseDescriptor) {
                foundLocalFile = true;
            } else if (descriptor instanceof MongoDBKnowledgeBase.MongoDBKnowledgeBaseDescriptor) {
                foundMongoDB = true;
            } else if (descriptor instanceof DifferentKnowledgeBase.DifferentKnowledgeBaseDescriptor) {
                foundDifferentKb = true;
            }
        }
        assertTrue("Expected to find a LocalFileKnowledgeBaseDescriptor", foundLocalFile);
        assertTrue("Expected to find a MongoDBKnowledgeBaseDescriptor", foundMongoDB);
        assertTrue("Expected to find a DifferentKnowledgeBaseDescriptor", foundDifferentKb);
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}. with the same
     * KnowledgeBase as before.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigure() throws Exception {
        KnowledgeBase prevKnowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        String expectedNoCauseMessage = "I am blinded!";
        StaplerRequest sreq = mock(StaplerRequest.class);
        LocalFileKnowledgeBase knowledgeBase = new LocalFileKnowledgeBase();
        when(sreq.bindJSON(eq(KnowledgeBase.class), isA(JSONObject.class))).thenReturn(knowledgeBase);

        JSONObject form = createForm(expectedNoCauseMessage, PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, null);

        PluginImpl.getInstance().configure(sreq, form);


        assertSame(prevKnowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertEquals(expectedNoCauseMessage, PluginImpl.getInstance().getNoCausesMessage());
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}.
     * with the same KnowledgeBase as before. And nrOfScanThreads set/"hacked" to -1
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigureLowScanThreads() throws Exception {
        KnowledgeBase prevKnowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        String expectedNoCauseMessage = "I am blinded!";
        StaplerRequest sreq = mock(StaplerRequest.class);
        LocalFileKnowledgeBase knowledgeBase = new LocalFileKnowledgeBase();
        when(sreq.bindJSON(eq(KnowledgeBase.class), isA(JSONObject.class))).thenReturn(knowledgeBase);

        JSONObject form = createForm(expectedNoCauseMessage, -1, null);

        PluginImpl.getInstance().configure(sreq, form);


        assertSame(prevKnowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertEquals(expectedNoCauseMessage, PluginImpl.getInstance().getNoCausesMessage());
        assertEquals(PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, PluginImpl.getInstance().getNrOfScanThreads());
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}. with a new
     * KnowledgeBase type.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigureConvert() throws Exception {
        KnowledgeBase prevKnowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        cause = prevKnowledgeBase.addCause(cause);
        StaplerRequest sreq = mock(StaplerRequest.class);
        DifferentKnowledgeBase knowledgeBase = new DifferentKnowledgeBase("Hello");
        when(sreq.bindJSON(eq(KnowledgeBase.class), isA(JSONObject.class))).thenReturn(knowledgeBase);

        JSONObject form = createForm("x", PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, true);

        PluginImpl.getInstance().configure(sreq, form);

        assertNotSame(prevKnowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertSame(knowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertEquals(1, knowledgeBase.getCauses().size());
        assertSame(cause, knowledgeBase.getCauses().iterator().next());

        //Check that the config page contains what we expect as well.
        HtmlPage page = jenkins.createWebClient().goTo("configure");
        assertConfigPageRendering(knowledgeBase, page);
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}. with the same
     * KnowledgeBase type but different configuration.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigureConvertSameType() throws Exception {
        DifferentKnowledgeBase prevKnowledgeBase = new DifferentKnowledgeBase("Original");
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        cause = prevKnowledgeBase.addCause(cause);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, prevKnowledgeBase);
        StaplerRequest sreq = mock(StaplerRequest.class);
        DifferentKnowledgeBase knowledgeBase = new DifferentKnowledgeBase("Hello Again");
        when(sreq.bindJSON(eq(KnowledgeBase.class), isA(JSONObject.class))).thenReturn(knowledgeBase);

        JSONObject form = createForm("x", PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, true);

        PluginImpl.getInstance().configure(sreq, form);

        assertNotSame(prevKnowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertSame(knowledgeBase, PluginImpl.getInstance().getKnowledgeBase());
        assertEquals(1, knowledgeBase.getCauses().size());
        assertSame(cause, knowledgeBase.getCauses().iterator().next());

        //Check that the config page contains what we expect as well.
        HtmlPage page = jenkins.createWebClient().goTo("configure");
        assertConfigPageRendering(knowledgeBase, page);
    }

    /**
     * Checks that the configuration page has the correct values visible and selected.
     *
     * @param knowledgeBase the configured knowledgeBase.
     * @param page          the html page (http://jenkins/configure).
     */
    private void assertConfigPageRendering(DifferentKnowledgeBase knowledgeBase, HtmlPage page) {
        //Check that the select box has the correct value
        HtmlElement element = getStorageTypeRow(page);
        HtmlElement settingsMainElement = element.getOneHtmlElementByAttribute("td", "class", "setting-main");
        HtmlSelect select = (HtmlSelect)settingsMainElement.getChildElements().iterator().next();
        assertEquals(knowledgeBase.getDescriptor().getDisplayName(), select.getSelectedOptions().get(0).getText());

        //Check that it has the someString input field with correct value
        DomNode container = settingsMainElement.getEnclosingElement("tr").getNextSibling();
        HtmlTable table = page.getDocumentElement().getOneHtmlElementByAttribute("table", "name", "knowledgeBase");
        assertSame("The table should be inside the dropDownContainer", container, table.getParentNode().getParentNode());
        HtmlTableCell cell = table.getCellAt(1, 2);
        HtmlTextInput someStringInput = (HtmlTextInput)cell.getAllHtmlChildElements().iterator().next();
        assertEquals(knowledgeBase.getSomeString(), someStringInput.getText());
    }

    /**
     * Find the table row where the text "Storage type" is present in a cell.
     *
     * @param page the page to search in.
     * @return the table row.
     */
    private HtmlElement getStorageTypeRow(HtmlPage page) {
        List<HtmlElement> elements = page.getDocumentElement().getElementsByAttribute("td", "class", "setting-name");
        for (HtmlElement element : elements) {
            if ("Storage type".equals(element.getTextContent())) {
                return element.getEnclosingElement("tr");
            }
        }
        return null;
    }

    /**
     * Creates a form to send to the configure method.
     *
     * @param expectedNoCauseMessage the text for {@link PluginImpl#noCausesMessage}.
     * @param convert                if convertion should be run or not, set to null no not put the value in the form.
     * @param nrOfScanThreads           the number of threads.
     * @return the form data.
     */
    private JSONObject createForm(String expectedNoCauseMessage, int nrOfScanThreads, Boolean convert) {
        JSONObject form = new JSONObject();
        form.put("noCausesMessage", expectedNoCauseMessage);
        form.put("globalEnabled", true);
        form.put("graphsEnabled", true);
        form.put("gerritTriggerEnabled", true);
        form.put("testResultParsingEnabled", true);
        form.put("testResultCategories", "foo bar");
        form.put("knowledgeBase", new JSONObject());
        form.put("nrOfScanThreads", nrOfScanThreads);
        form.put("maximumNumberOfWorkerThreads", ScanOnDemandVariables.DEFAULT_MAXIMUM_SOD_WORKER_THREADS);
        form.put("minimumNumberOfWorkerThreads", ScanOnDemandVariables.DEFAULT_MINIMUM_SOD_WORKER_THREADS);
        form.put("threadKeepAliveTime", ScanOnDemandVariables.DEFAULT_SOD_THREADS_KEEP_ALIVE_TIME);
        form.put("waitForJobShutdownTime", ScanOnDemandVariables.DEFAULT_SOD_WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT);
        form.put("corePoolNumberOfThreads", ScanOnDemandVariables.DEFAULT_SOD_COREPOOL_THREADS);
        if (convert != null) {
            form.put("convertOldKb", convert);
        }
        return form;
    }
}
