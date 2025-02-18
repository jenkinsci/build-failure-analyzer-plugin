/*
 * The MIT License
 *
 * Copyright 2014 Stellar Science Ltd Co
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
package com.sonyericsson.jenkins.plugins.bfa.tokens;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.sonyericsson.jenkins.plugins.bfa.BuildFailureScannerHudsonTest;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author K. R. Walker &lt;krwalker@stellarscience.com&gt;
 */
@WithJenkins
class TokenTest {
    private static final Logger logger = Logger.getLogger(PluginImpl.class.getName());
    private static final String ERROR = "ERROR";

    private final TaskListener listener = new LogTaskListener(logger, Level.INFO);

    private FreeStyleProject project;
    private FreeStyleBuild noCauseBuild;
    private FreeStyleBuild causeBuild;
    private FreeStyleBuild htmlCauseBuild;

    @BeforeEach
     void setUp(JenkinsRule j) throws Exception {
        final int timeout = 10;
        final int quietPeriod = 0;

        project = j.createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(quietPeriod);

        noCauseBuild  = noCauseBuildFuture.get(timeout, TimeUnit.SECONDS);

        BuildFailureScannerHudsonTest.configureCauseAndIndication("error",
                "There was an error.", "comment", "category",
                new BuildLogIndication(".*ERROR.*"));

        Future<FreeStyleBuild> buildFuture = project.scheduleBuild2(quietPeriod);
        causeBuild = buildFuture.get(timeout, TimeUnit.SECONDS);

        BuildFailureScannerHudsonTest.configureCauseAndIndication("error",
                "There was an <b>error</b>.", "comment", "category",
                new BuildLogIndication(".*HTML.*"));
        project.getBuildersList().clear();
        project.getBuildersList().add(new PrintToLogBuilder("HTML"));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));

        buildFuture = project.scheduleBuild2(quietPeriod);
        htmlCauseBuild = buildFuture.get(timeout, TimeUnit.SECONDS);
    }

    /**
     * Test that the BUILD_FAILURE_ANALYZER token gets replaced with failure cause text as configured.
     * @throws Exception if necessary
     */
    @Test
    void testExpandAllNoError() throws Exception {
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Test that the BUILD_FAILURE_ANALYZER token gets replaced with failure cause text as configured.
     * @throws Exception if necessary
     */
    @Test
    void testExpandNoError() throws Exception {
        final String defaultNoResult = TokenMacro.expand(noCauseBuild, noCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is a failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllError() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expandAll(causeBuild, listener, "${BUILD_FAILURE_ANALYZER}");
        //System.out.println("Default:\n[" + defaults + "]");
        assertTrue(defaults.contains("Identified problems:"), "Default has title");
        assertTrue(defaults.contains("There was an error."), "Default has cause");
        assertTrue(defaults.contains("Indication 1"), "Default has indications");
        assertFalse(defaults.contains("<li>"), "Default is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandError() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER}");
        System.out.println("Default:\n[" + defaults + "]");
        assertTrue(defaults.contains("Identified problems:"), "Default has title");
        assertTrue(defaults.contains("There was an error."), "Default has cause");
        assertTrue(defaults.contains("Indication 1"), "Default has indications");
        assertFalse(defaults.contains("<li>"), "Default is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllnErrorWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue(plainFull.contains("Identified problems:"), "Plaintext full has title");
        assertTrue(plainFull.contains("There was an error."), "Plaintext full has cause");
        assertTrue(plainFull.contains("Indication 1"), "Plaintext full has indications");
        assertFalse(plainFull.contains("<li>"), "Plaintext full is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandnErrorWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue(plainFull.contains("Identified problems:"), "Plaintext full has title");
        assertTrue(plainFull.contains("There was an error."), "Plaintext full has cause");
        assertTrue(plainFull.contains("Indication 1"), "Plaintext full has indications");
        assertFalse(plainFull.contains("<li>"), "Plaintext full is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllHtml() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expandAll(htmlCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER,escapeHtml=true}");
        //System.out.println("Default:\n[" + defaults + "]");
        assertTrue(defaults.contains("Identified problems:"), "Default has title");
        assertTrue(defaults.contains("There was an &lt;b&gt;error&lt;/b&gt;."), "Default has cause");
        assertTrue(defaults.contains("Indication 1"), "Default has indications");
        assertFalse(defaults.contains("<li>"), "Default is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandHtml() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expand(htmlCauseBuild, htmlCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER,escapeHtml=true}");
        //System.out.println("Default:\n[" + defaults + "]");
        assertTrue(defaults.contains("Identified problems:"), "Default has title");
        assertTrue(defaults.contains("There was an &lt;b&gt;error&lt;/b&gt;."), "Default has cause");
        assertTrue(defaults.contains("Indication 1"), "Default has indications");
        assertFalse(defaults.contains("<li>"), "Default is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expandAll(htmlCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true,escapeHtml=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue(plainFull.contains("Identified problems:"), "Plaintext full has title");
        assertTrue(plainFull.contains("There was an &lt;b&gt;error&lt;/b&gt;."), "Plaintext full has cause");
        assertTrue(plainFull.contains("Indication 1"), "Plaintext full has indications");
        assertFalse(plainFull.contains("<li>"), "Plaintext full is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandnHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expand(htmlCauseBuild, htmlCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true,escapeHtml=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue(plainFull.contains("Identified problems:"), "Plaintext full has title");
        assertTrue(plainFull.contains("There was an &lt;b&gt;error&lt;/b&gt;."), "Plaintext full has cause");
        assertTrue(plainFull.contains("Indication 1"), "Plaintext full has indications");
        assertFalse(plainFull.contains("<li>"), "Plaintext full is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title, indications, and wraps the width.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllErrorWithTitleAndIndicationsAndWrap() throws Exception {
        final int expectedOutputLineCount = 7;
        final String plainFullWrapped = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true, wrapWidth=8}");
        System.out.println("Plaintext full wrapped:\n[" + plainFullWrapped + "]");
        assertTrue(plainFullWrapped.contains("Identified problems:"), "Plaintext full wrapped has title");
        assertTrue(plainFullWrapped.contains("error."), "Plaintext full wrapped has cause");
        assertTrue(plainFullWrapped.contains("Indication 1"), "Plaintext full wrapped has indications");
        assertFalse(plainFullWrapped.contains("<li>"), "Plaintext full wrapped is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFullWrapped)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title, indications, and wraps the width.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandErrorWithTitleAndIndicationsAndWrap() throws Exception {
        final int expectedOutputLineCount = 7;
        final String plainFullWrapped = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true, wrapWidth=8}");
        System.out.println("Plaintext full wrapped:\n[" + plainFullWrapped + "]");
        assertTrue(plainFullWrapped.contains("Identified problems:"), "Plaintext full wrapped has title");
        assertTrue(plainFullWrapped.contains("error."), "Plaintext full wrapped has cause");
        assertTrue(plainFullWrapped.contains("Indication 1"), "Plaintext full wrapped has indications");
        assertFalse(plainFullWrapped.contains("<li>"), "Plaintext full wrapped is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFullWrapped)));
    }

    /**
     * Tests the expansion when there is a failure that excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllErrorNoTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String plainMinimal = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        System.out.println("Plaintext minimal:\n[" + plainMinimal + "]");
        assertFalse(plainMinimal.contains("Identified problems:"), "Plaintext minimal does not have title");
        assertTrue(plainMinimal.contains("There was an error."), "Plaintext minimal has cause");
        assertFalse(plainMinimal.contains("Indication 1"), "Plaintext minimal does not have indications");
        assertFalse(plainMinimal.contains("<li>"), "Plaintext minimal is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandErrorNoTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String plainMinimal = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        System.out.println("Plaintext minimal:\n[" + plainMinimal + "]");
        assertFalse(plainMinimal.contains("Identified problems:"), "Plaintext minimal does not have title");
        assertTrue(plainMinimal.contains("There was an error."), "Plaintext minimal has cause");
        assertFalse(plainMinimal.contains("Indication 1"), "Plaintext minimal does not have indications");
        assertFalse(plainMinimal.contains("<li>"), "Plaintext minimal is not HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllErrorHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlFull = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, includeIndications=true}");
        System.out.println("HTML full:\n[" + htmlFull + "]");
        assertTrue(htmlFull.contains("Identified problems:"), "HTML full has title");
        assertTrue(htmlFull.contains("There was an error."), "HTML full has cause");
        assertTrue(htmlFull.contains("Indication 1"), "HTML full has indications");
        assertTrue(htmlFull.contains("<li>"), "HTML full is HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlFull)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandErrorHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlFull = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, "
                        + "includeIndications=true}");
        System.out.println("HTML full:\n[" + htmlFull + "]");
        assertTrue(htmlFull.contains("Identified problems:"), "HTML full has title");
        assertTrue(htmlFull.contains("There was an error."), "HTML full has cause");
        assertTrue(htmlFull.contains("Indication 1"), "HTML full has indications");
        assertTrue(htmlFull.contains("<li>"), "HTML full is HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlFull)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllErrorHtmlWithTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlMinimal = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, "
                    + "includeIndications=false}");
        System.out.println("HTML minimal:\n[" + htmlMinimal + "]");
        assertFalse(htmlMinimal.contains("Identified problems:"), "HTML minimal does not have title");
        assertTrue(htmlMinimal.contains("There was an error."), "HTML minimal has cause");
        assertFalse(htmlMinimal.contains("Indication 1"), "HTML minimal does not have indications");
        assertTrue(htmlMinimal.contains("<li>"), "HTML minimal is HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandErrorHtmlWithTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlMinimal = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, "
                        + "includeIndications=false}");
        System.out.println("HTML minimal:\n[" + htmlMinimal + "]");
        assertFalse(htmlMinimal.contains("Identified problems:"), "HTML minimal does not have title");
        assertTrue(htmlMinimal.contains("There was an error."), "HTML minimal has cause");
        assertFalse(htmlMinimal.contains("Indication 1"), "HTML minimal does not have indications");
        assertTrue(htmlMinimal.contains("<li>"), "HTML minimal is HTML");
        assertEquals(expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlMinimal)));
    }

    /**
     * Tests the expansion when there is no failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllNoFailureWithDefaultEmptyText() throws Exception {
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandNoFailureWithDefaultEmptyText() throws Exception {
        final String defaultNoResult = TokenMacro.expand(noCauseBuild, noCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to the empty string.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllNoFailureWithEmptyText() throws Exception {
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"\"}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to the empty string.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandNoFailureWithEmptyText() throws Exception {
        final String defaultNoResult = TokenMacro.expand(noCauseBuild, noCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"\"}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to something.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandAllNoFailureWithText() throws Exception {
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"Sample text with <b>html</b>\"}");
        assertEquals("Sample text with <b>html</b>", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to something.
     *
     * @throws Exception If necessary
     */
    @Test
    void testExpandNoFailureWithTextEscapeHtml() throws Exception {
        final String defaultNoResult = TokenMacro.expand(noCauseBuild, noCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, escapeHtml=true, noFailureText=\"Sample text with <b>html</b>\"}");
        assertEquals("Sample text with &lt;b&gt;html&lt;/b&gt;", defaultNoResult);
    }

}
