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
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author K. R. Walker &lt;krwalker@stellarscience.com&gt;
 */
public class TokenTest extends HudsonTestCase {
    private static final Logger logger = Logger.getLogger(PluginImpl.class.getName());
    private static final String ERROR = "ERROR";

    private final TaskListener listener = new LogTaskListener(logger, Level.INFO);

    private FreeStyleProject project;
    private FreeStyleBuild noCauseBuild;
    private FreeStyleBuild causeBuild;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final int timeout = 10;
        final int quietPeriod = 0;

        project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(quietPeriod);

        noCauseBuild  = noCauseBuildFuture.get(timeout, TimeUnit.SECONDS);

        BuildFailureScannerHudsonTest.configureCauseAndIndication("error",
                "There was an error.", "comment", "category",
                new BuildLogIndication(".*ERROR.*"));

        final Future<FreeStyleBuild> buildFuture = project.scheduleBuild2(quietPeriod);
        causeBuild = buildFuture.get(timeout, TimeUnit.SECONDS);
    }

    /**
     * Test that the BUILD_FAILURE_ANALYZER token gets replaced with failure cause text as configured.
     * @throws Exception if necessary
     */
    @Test
    public void testExpandAllNoError() throws Exception {
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Test that the BUILD_FAILURE_ANALYZER token gets replaced with failure cause text as configured.
     * @throws Exception if necessary
     */
    @Test
    public void testExpandNoError() throws Exception {
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
    public void testExpandAllError() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expandAll(causeBuild, listener, "${BUILD_FAILURE_ANALYZER}");
        System.out.println("Default:\n[" + defaults + "]");
        assertTrue("Default has title", defaults.contains("Identified problems:"));
        assertTrue("Default has cause", defaults.contains("There was an error."));
        assertTrue("Default has indications", defaults.contains("Indication 1"));
        assertTrue("Default is not HTML", !defaults.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandError() throws Exception {
        final int expectedOutputLineCount = 4;
        final String defaults = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER}");
        System.out.println("Default:\n[" + defaults + "]");
        assertTrue("Default has title", defaults.contains("Identified problems:"));
        assertTrue("Default has cause", defaults.contains("There was an error."));
        assertTrue("Default has indications", defaults.contains("Indication 1"));
        assertTrue("Default is not HTML", !defaults.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(defaults)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllnErrorWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue("Plaintext full has title", plainFull.contains("Identified problems:"));
        assertTrue("Plaintext full has cause", plainFull.contains("There was an error."));
        assertTrue("Plaintext full has indications", plainFull.contains("Indication 1"));
        assertTrue("Plaintext full is not HTML", !plainFull.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandnErrorWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 4;
        final String plainFull = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue("Plaintext full has title", plainFull.contains("Identified problems:"));
        assertTrue("Plaintext full has cause", plainFull.contains("There was an error."));
        assertTrue("Plaintext full has indications", plainFull.contains("Indication 1"));
        assertTrue("Plaintext full is not HTML", !plainFull.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFull)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title, indications, and wraps the width.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllErrorWithTitleAndIndicationsAndWrap() throws Exception {
        final int expectedOutputLineCount = 7;
        final String plainFullWrapped = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true, wrapWidth=8}");
        System.out.println("Plaintext full wrapped:\n[" + plainFullWrapped + "]");
        assertTrue("Plaintext full wrapped has title", plainFullWrapped.contains("Identified problems:"));
        assertTrue("Plaintext full wrapped has cause", plainFullWrapped.contains("error."));
        assertTrue("Plaintext full wrapped has indications", plainFullWrapped.contains("Indication 1"));
        assertTrue("Plaintext full wrapped is not HTML", !plainFullWrapped.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFullWrapped)));
    }

    /**
     * Tests the expansion when there is a failure that includes the title, indications, and wraps the width.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandErrorWithTitleAndIndicationsAndWrap() throws Exception {
        final int expectedOutputLineCount = 7;
        final String plainFullWrapped = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true, wrapWidth=8}");
        System.out.println("Plaintext full wrapped:\n[" + plainFullWrapped + "]");
        assertTrue("Plaintext full wrapped has title", plainFullWrapped.contains("Identified problems:"));
        assertTrue("Plaintext full wrapped has cause", plainFullWrapped.contains("error."));
        assertTrue("Plaintext full wrapped has indications", plainFullWrapped.contains("Indication 1"));
        assertTrue("Plaintext full wrapped is not HTML", !plainFullWrapped.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainFullWrapped)));
    }

    /**
     * Tests the expansion when there is a failure that excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllErrorNoTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String plainMinimal = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        System.out.println("Plaintext minimal:\n[" + plainMinimal + "]");
        assertTrue("Plaintext minimal does not have title", !plainMinimal.contains("Identified problems:"));
        assertTrue("Plaintext minimal has cause", plainMinimal.contains("There was an error."));
        assertTrue("Plaintext minimal does not have indications", !plainMinimal.contains("Indication 1"));
        assertTrue("Plaintext minimal is not HTML", !plainMinimal.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandErrorNoTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String plainMinimal = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        System.out.println("Plaintext minimal:\n[" + plainMinimal + "]");
        assertTrue("Plaintext minimal does not have title", !plainMinimal.contains("Identified problems:"));
        assertTrue("Plaintext minimal has cause", plainMinimal.contains("There was an error."));
        assertTrue("Plaintext minimal does not have indications", !plainMinimal.contains("Indication 1"));
        assertTrue("Plaintext minimal is not HTML", !plainMinimal.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(plainMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllErrorHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlFull = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, includeIndications=true}");
        System.out.println("HTML full:\n[" + htmlFull + "]");
        assertTrue("HTML full has title", htmlFull.contains("Identified problems:"));
        assertTrue("HTML full has cause", htmlFull.contains("There was an error."));
        assertTrue("HTML full has indications", htmlFull.contains("Indication 1"));
        assertTrue("HTML full is HTML", htmlFull.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlFull)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which includes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandErrorHtmlWithTitleAndIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlFull = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, "
                        + "includeIndications=true}");
        System.out.println("HTML full:\n[" + htmlFull + "]");
        assertTrue("HTML full has title", htmlFull.contains("Identified problems:"));
        assertTrue("HTML full has cause", htmlFull.contains("There was an error."));
        assertTrue("HTML full has indications", htmlFull.contains("Indication 1"));
        assertTrue("HTML full is HTML", htmlFull.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlFull)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllErrorHtmlWithTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlMinimal = TokenMacro.expandAll(causeBuild, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, "
                    + "includeIndications=false}");
        System.out.println("HTML minimal:\n[" + htmlMinimal + "]");
        assertTrue("HTML minimal does not have title", !htmlMinimal.contains("Identified problems:"));
        assertTrue("HTML minimal has cause", htmlMinimal.contains("There was an error."));
        assertTrue("HTML minimal does not have indications", !htmlMinimal.contains("Indication 1"));
        assertTrue("HTML minimal is HTML", htmlMinimal.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlMinimal)));
    }

    /**
     * Tests the expansion when there is a failure that uses HTML which excludes the title and indications.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandErrorHtmlWithTitleNoIndications() throws Exception {
        final int expectedOutputLineCount = 1;
        final String htmlMinimal = TokenMacro.expand(causeBuild, causeBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, "
                        + "includeIndications=false}");
        System.out.println("HTML minimal:\n[" + htmlMinimal + "]");
        assertTrue("HTML minimal does not have title", !htmlMinimal.contains("Identified problems:"));
        assertTrue("HTML minimal has cause", htmlMinimal.contains("There was an error."));
        assertTrue("HTML minimal does not have indications", !htmlMinimal.contains("Indication 1"));
        assertTrue("HTML minimal is HTML", htmlMinimal.contains("<li>"));
        assertEquals("", expectedOutputLineCount, Iterables.size(Splitter.on('\n')
                .omitEmptyStrings().split(htmlMinimal)));
    }

    /**
     * Tests the expansion when there is no failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testExpandAllNoFailureWithDefaultEmptyText() throws Exception {
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
    public void testExpandNoFailureWithDefaultEmptyText() throws Exception {
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
    public void testExpandAllNoFailureWithEmptyText() throws Exception {
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
    public void testExpandNoFailureWithEmptyText() throws Exception {
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
    public void testExpandAllNoFailureWithText() throws Exception {
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
    public void testExpandNoFailureWithText() throws Exception {
        final String defaultNoResult = TokenMacro.expand(noCauseBuild, noCauseBuild.getWorkspace(), listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"Sample text with <b>html</b>\"}");
        assertEquals("Sample text with <b>html</b>", defaultNoResult);
    }

}
