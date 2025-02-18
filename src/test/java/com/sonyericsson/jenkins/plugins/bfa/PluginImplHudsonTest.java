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

import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextInput;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.DifferentKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import hudson.ExtensionList;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.WebApp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TestCases for {@link PluginImpl}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@WithJenkins
class PluginImplHudsonTest {

    //CS IGNORE MagicNumber FOR NEXT 20 LINES. REASON: Random test data

    /**
     * Tests that boolean configure parameters are persisted.
     *
     * @param jenkins
     */
    @Test
    void testBooleanConfigPersistence(JenkinsRule jenkins) {
        PluginImpl instance = PluginImpl.getInstance();
        // assert default config
        assertTrue(instance.isGlobalEnabled(), "globalEnabled: default value is true");
        assertFalse(instance.isTestResultParsingEnabled(), "testResultParsingEnabled: default value is false");
        assertTrue(instance.isGerritTriggerEnabled(), "gerritTriggerEnabled: default value is true");
        assertFalse(instance.isDoNotAnalyzeAbortedJob(), "doNotAnalyzeAbortedJob: default value is false");
        assertFalse(instance.isGraphsEnabled(), "graphsEnabled: default value is false");
        assertTrue(instance.isNoCausesEnabled(), "noCausesEnabled: default value is true");
        assertFalse(instance.isMetricSquashingEnabled(), "metricSquashingEnabled: default value is false");
        // to ever get graphsEnabled, we'll need a KB with enableStatistics, like MongoDBKB with the right option
        MongoDBKnowledgeBase mongoKB = new MongoDBKnowledgeBase("host", 27017, "dbname", "username",
                Secret.fromString("password"), true, true);
        instance.setKnowledgeBase(mongoKB);
        // need an actual StaplerRequest2 implementation to test we're using bindJSON correctly
        WebApp webapp = new WebApp(mock(ServletContext.class));
        Stapler stapler = mock(Stapler.class);
        when(stapler.getWebApp()).thenReturn(webapp);
        StaplerRequest2 sreq = new RequestImpl(stapler, mock(HttpServletRequest.class), Collections.emptyList(), null);
        // flip configuration of all boolean values
        JSONObject form = new JSONObject();
        form.put("globalEnabled", !instance.isGlobalEnabled());
        form.put("testResultParsingEnabled", !instance.isTestResultParsingEnabled());
        form.put("gerritTriggerEnabled", !instance.isGerritTriggerEnabled());
        form.put("doNotAnalyzeAbortedJob", !instance.isDoNotAnalyzeAbortedJob());
        form.put("graphsEnabled", !instance.isGraphsEnabled());
        form.put("noCausesEnabled", !instance.isNoCausesEnabled());
        form.put("metricSquashingEnabled", !instance.isMetricSquashingEnabled());
        instance.configure(sreq, form);
        // assert opposite config
        assertFalse(instance.isGlobalEnabled(), "globalEnabled: opposite value is false");
        assertTrue(instance.isTestResultParsingEnabled(), "testResultParsingEnabled: opposite value is true");
        assertFalse(instance.isGerritTriggerEnabled(), "gerritTriggerEnabled: opposite value is false");
        assertTrue(instance.isDoNotAnalyzeAbortedJob(), "doNotAnalyzeAbortedJob: opposite value is true");
        assertTrue(instance.isGraphsEnabled(), "graphsEnabled: opposite value is true");
        assertFalse(instance.isNoCausesEnabled(), "noCausesEnabled: opposite value is false");
        assertTrue(instance.isMetricSquashingEnabled(), "metricSquashingEnabled: opposite value is true");
    }

    /**
     * Tests that {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getKnowledgeBaseDescriptors()} contains the
     * default descriptors.
     *
     * @param jenkins
     */
    @Test
    void testGetKnowledgeBaseDescriptors(JenkinsRule jenkins) {
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
        assertTrue(foundLocalFile, "Expected to find a LocalFileKnowledgeBaseDescriptor");
        assertTrue(foundMongoDB, "Expected to find a MongoDBKnowledgeBaseDescriptor");
        assertTrue(foundDifferentKb, "Expected to find a DifferentKnowledgeBaseDescriptor");
    }

    /**
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)}. with a new
     * KnowledgeBase type.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testConfigureConvert(JenkinsRule jenkins) throws Exception {
        PluginImpl instance = PluginImpl.getInstance();
        KnowledgeBase prevKnowledgeBase = instance.getKnowledgeBase();
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        cause = prevKnowledgeBase.addCause(cause);
        StaplerRequest2 sreq = mock(StaplerRequest2.class);
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
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)}. with the same
     * KnowledgeBase type but different configuration.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testConfigureConvertSameType(JenkinsRule jenkins) throws Exception {
        DifferentKnowledgeBase prevKnowledgeBase = new DifferentKnowledgeBase("Original");
        FailureCause cause = new FailureCause("Olle", "Olle");
        cause.addIndication(new BuildLogIndication(".*olle"));
        cause = prevKnowledgeBase.addCause(cause);
        PluginImpl instance = PluginImpl.getInstance();
        Whitebox.setInternalState(instance, KnowledgeBase.class, prevKnowledgeBase);
        StaplerRequest2 sreq = mock(StaplerRequest2.class);
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
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)}.
     * Tests that a LocalFileKnowledgebase is preserved through a reconfigure without changes.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testConfigureIdenticalLocalKB(JenkinsRule jenkins) throws Exception {
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
     * Tests {@link PluginImpl#configure(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)}.
     * Tests that a MongoDBKnowledgebase is preserved through a reconfigure without changes.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @Test
    void testConfigureIdenticalMongoKB(JenkinsRule jenkins) throws Exception {
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
    private static void assertConfigPageRendering(DifferentKnowledgeBase knowledgeBase, HtmlPage page) {
        //Check that the select box has the correct value
        HtmlElement element = getStorageTypeRow(page);
        assertNotNull(element, "Should have found the base div");
        HtmlElement settingsMainElement = element.getOneHtmlElementByAttribute("div", "class", "jenkins-select");
        HtmlSelect select = (HtmlSelect)settingsMainElement.getChildElements().iterator().next();
        assertEquals(knowledgeBase.getDescriptor().getDisplayName(), select.getSelectedOptions().get(0).getText());

        //Check that it has the someString input field with correct value
        DomNode container = settingsMainElement.getEnclosingElement("div").getNextSibling();
        HtmlElement table = page.getDocumentElement().getOneHtmlElementByAttribute("div", "name", "knowledgeBase");
        assertSame(container, table.getParentNode().getParentNode(), "The table should be inside the dropDownContainer");
        final HtmlTextInput someStringInput = table.getOneHtmlElementByAttribute("input", "name", "_.someString");
        assertNotNull(someStringInput, "Should have found some text input");
        assertEquals(knowledgeBase.getSomeString(), someStringInput.getText());
    }

    /**
     * Find the table row where the text "Storage type" is present in a cell.
     *
     * @param page the page to search in.
     * @return the table row.
     */
    private static HtmlElement getStorageTypeRow(HtmlPage page) {
        List<HtmlElement> elements = page.getDocumentElement().getElementsByAttribute(
                "div", "class", "jenkins-form-label help-sibling");
        for (HtmlElement element : elements) {
            if ("Storage type".equals(StringUtils.trim(element.getTextContent()))) {
                return element.getEnclosingElement("div");
            }
        }
        return null;
    }

    /**
     * Creates a form to send to the configure method.
     *
     * @param expectedNoCauseMessage the text for noCausesMessage.
     * @param convert                if convertion should be run or not, set to null no not put the value in the form.
     * @param nrOfScanThreads        the number of threads.
     * @return the form data.
     */
    private static JSONObject createForm(String expectedNoCauseMessage, int nrOfScanThreads, Boolean convert) {
        JSONObject form = new JSONObject();
        form.put("noCausesMessage", expectedNoCauseMessage);
        form.put("noCausesEnabled", true);
        form.put("globalEnabled", true);
        form.put("graphsEnabled", true);
        form.put("gerritTriggerEnabled", true);
        form.put("slackNotifEnabled", true);
        form.put("slackChannelName", PluginImpl.DEFAULT_SLACK_CHANNEL);
        form.put("slackFailureCategories", PluginImpl.DEFAULT_SLACK_FAILURE_CATEGORIES);
        form.put("testResultParsingEnabled", true);
        form.put("testResultCategories", "foo bar");
        form.put("metricSquashingEnabled", false);
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
