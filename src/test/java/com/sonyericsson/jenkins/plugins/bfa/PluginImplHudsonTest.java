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
import com.gargoylesoftware.htmlunit.html.HtmlForm;
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
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebApp;
import org.powermock.reflect.Whitebox;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TestCases for {@link PluginImpl}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImplHudsonTest {

    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule jenkins = new JenkinsRule();

    //CS IGNORE MagicNumber FOR NEXT 20 LINES. REASON: Random test data

    @Test
    public void testBooleanConfigPersistence() throws Exception {
        PluginImpl instance = PluginImpl.getInstance();
        // assert default config
        assertTrue("globalEnabled: default value is true", instance.isGlobalEnabled());
        assertFalse("testResultParsingEnabled: default value is false", instance.isTestResultParsingEnabled());
        assertTrue("gerritTriggerEnabled: default value is true", instance.isGerritTriggerEnabled());
        assertFalse("doNotAnalyzeAbortedJob: default value is false", instance.isDoNotAnalyzeAbortedJob());
        assertFalse("graphsEnabled: default value is false", instance.isGraphsEnabled());
        // to ever get graphsEnabled, we'll need a KB with enableStatistics, like MongoDBKB with the right option
        MongoDBKnowledgeBase mongoKB = new MongoDBKnowledgeBase("host", 27017, "dbname", "username",
                Secret.fromString("password"), true, true);
        instance.setKnowledgeBase(mongoKB);
        // need an actual StaplerRequest implementation to test we're using bindJSON correctly
        WebApp webapp = new WebApp(mock(ServletContext.class));
        Stapler stapler = mock(Stapler.class);
        when(stapler.getWebApp()).thenReturn(webapp);
        StaplerRequest sreq = new RequestImpl(stapler, mock(HttpServletRequest.class), Collections.emptyList(), null);
        // flip configuration of all boolean values
        JSONObject form = new JSONObject();
        form.put("globalEnabled", !instance.isGlobalEnabled());
        form.put("testResultParsingEnabled", !instance.isTestResultParsingEnabled());
        form.put("gerritTriggerEnabled", !instance.isGerritTriggerEnabled());
        form.put("doNotAnalyzeAbortedJob", !instance.isDoNotAnalyzeAbortedJob());
        form.put("graphsEnabled", !instance.isGraphsEnabled());
        instance.configure(sreq, form);
        // assert opposite config
        assertFalse("globalEnabled: opposite value is false", instance.isGlobalEnabled());
        assertTrue("testResultParsingEnabled: opposite value is true", instance.isTestResultParsingEnabled());
        assertFalse("gerritTriggerEnabled: opposite value is false", instance.isGerritTriggerEnabled());
        assertTrue("doNotAnalyzeAbortedJob: opposite value is true", instance.isDoNotAnalyzeAbortedJob());
        assertTrue("graphsEnabled: opposite value is true", instance.isGraphsEnabled());
    }

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
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}. with a new
     * KnowledgeBase type.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigureConvert() throws Exception {
        PluginImpl instance = PluginImpl.getInstance();
        KnowledgeBase prevKnowledgeBase = instance.getKnowledgeBase();
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        cause = prevKnowledgeBase.addCause(cause);
        StaplerRequest sreq = mock(StaplerRequest.class);
        DifferentKnowledgeBase knowledgeBase = new DifferentKnowledgeBase("Hello");

        JSONObject form = createForm("x", PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, true);
        doAnswer(invocationOnMock -> {
            instance.setKnowledgeBase(knowledgeBase);
            return null;
        }).when(sreq).bindJSON(eq(instance), eq(form));

        instance.configure(sreq, form);

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
        PluginImpl instance = PluginImpl.getInstance();
        Whitebox.setInternalState(instance, KnowledgeBase.class, prevKnowledgeBase);
        StaplerRequest sreq = mock(StaplerRequest.class);
        DifferentKnowledgeBase knowledgeBase = new DifferentKnowledgeBase("Hello Again");
        when(sreq.bindJSON(eq(KnowledgeBase.class), isA(JSONObject.class))).thenReturn(knowledgeBase);

        doAnswer(invocationOnMock -> {
            instance.setKnowledgeBase(knowledgeBase);
            return null;
        }).when(sreq).bindJSON(eq(instance), any());

        JSONObject form = createForm("x", PluginImpl.DEFAULT_NR_OF_SCAN_THREADS, true);

        instance.configure(sreq, form);

        assertNotSame(prevKnowledgeBase, instance.getKnowledgeBase());
        assertSame(knowledgeBase, instance.getKnowledgeBase());
        assertEquals(1, knowledgeBase.getCauses().size());
        assertSame(cause, knowledgeBase.getCauses().iterator().next());

        //Check that the config page contains what we expect as well.
        HtmlPage page = jenkins.createWebClient().goTo("configure");
        assertConfigPageRendering(knowledgeBase, page);
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}.
     * Tests that a LocalFileKnowledgebase is preserved through a reconfigure without changes.
     * @throws Exception if so.
     */
    @Test
    public void testConfigureIdenticalLocalKB() throws Exception {
        LocalFileKnowledgeBase prevKnowledgeBase = new LocalFileKnowledgeBase();
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        prevKnowledgeBase.addCause(cause);
        PluginImpl instance = PluginImpl.getInstance();
        instance.setKnowledgeBase(prevKnowledgeBase);
        HtmlForm form = jenkins.createWebClient().goTo("configure").getFormByName("config");
        jenkins.submit(form);
        LocalFileKnowledgeBase knowledgeBase = (LocalFileKnowledgeBase)instance.getKnowledgeBase();

        //assert that nothing in the KB has changed, since a change was made from one Local KB to another.
        assertSame(prevKnowledgeBase, knowledgeBase);
        assertEquals(1, knowledgeBase.getCauses().size());
        assertSame(cause, knowledgeBase.getCauses().iterator().next());
    }

    //CS IGNORE MagicNumber FOR NEXT 17 LINES. REASON: Random test data

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}.
     * Tests that a MongoDBKnowledgebase is preserved through a reconfigure without changes.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConfigureIdenticalMongoKB() throws Exception {
        MongoDBKnowledgeBase prevKnowledgeBase = new MongoDBKnowledgeBase("host",
                27017,
                "dbname",
                "username",
                Secret.fromString("password"),
                false,
                true);
        PluginImpl instance = PluginImpl.getInstance();
        instance.setKnowledgeBase(prevKnowledgeBase);
        HtmlForm form = jenkins.createWebClient().goTo("configure").getFormByName("config");
        jenkins.submit(form);
        MongoDBKnowledgeBase newkb = (MongoDBKnowledgeBase)instance.getKnowledgeBase();
        assertSame(prevKnowledgeBase, newkb);
        //make sure that the values are preserved and not replaced by defaults.
        assertFalse(newkb.isEnableStatistics());
        assertTrue(newkb.isSuccessfulLogging());
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
        HtmlTextInput someStringInput = (HtmlTextInput)cell.getHtmlElementDescendants().iterator().next();
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
     * @param expectedNoCauseMessage the text for noCausesMessage.
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
