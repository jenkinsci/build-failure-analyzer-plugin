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

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.sonyericsson.jenkins.plugins.bfa.BuildFailureScannerHudsonTest;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.PrintToLogBuilder;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockBuilder;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;

/**
 * @author K. R. Walker &lt;krwalker@stellarscience.com&gt;
 */
public class TokenTest extends HudsonTestCase {
    private static final Logger logger = Logger.getLogger(PluginImpl.class.getName());
    private static final String ERROR = "ERROR";

    private final TaskListener listener = new LogTaskListener(logger, Level.INFO);

    /**
     * Test that the BUILD_FAILURE_ANALYZER token gets replaced with failure cause text as configured.
     * @throws Exception if necessary
     */
    @Test
    public void testToken() throws Exception {
        // CS IGNORE MagicNumberCheck FOR NEXT 63 LINES. REASON: Test data.
        final FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild noCauseBuild = noCauseBuildFuture.get(10, TimeUnit.SECONDS);
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener, "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
        BuildFailureScannerHudsonTest.configureCauseAndIndication("error", "There was an error.", "comment", "category",
            new BuildLogIndication(".*ERROR.*"));
        final Future<FreeStyleBuild> buildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild build = buildFuture.get(10, TimeUnit.SECONDS);
        final String defaults = TokenMacro.expandAll(build, listener, "${BUILD_FAILURE_ANALYZER}");
        System.out.println("Default:\n[" + defaults + "]");
        assertTrue("Default has title", defaults.contains("Identified problems:"));
        assertTrue("Default has cause", defaults.contains("There was an error."));
        assertTrue("Default has indications", defaults.contains("Indication 1"));
        assertTrue("Default is not HTML", !defaults.contains("<li>"));
        assertEquals("", 4, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(defaults)));

        final String plainFull = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        System.out.println("Plaintext full:\n[" + plainFull + "]");
        assertTrue("Plaintext full has title", plainFull.contains("Identified problems:"));
        assertTrue("Plaintext full has cause", plainFull.contains("There was an error."));
        assertTrue("Plaintext full has indications", plainFull.contains("Indication 1"));
        assertTrue("Plaintext full is not HTML", !plainFull.contains("<li>"));
        assertEquals("", 4, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(plainFull)));

        final String plainFullWrapped = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true, wrapWidth=8}");
        System.out.println("Plaintext full wrapped:\n[" + plainFullWrapped + "]");
        assertTrue("Plaintext full wrapped has title", plainFullWrapped.contains("Identified problems:"));
        assertTrue("Plaintext full wrapped has cause", plainFullWrapped.contains("error."));
        assertTrue("Plaintext full wrapped has indications", plainFullWrapped.contains("Indication 1"));
        assertTrue("Plaintext full wrapped is not HTML", !plainFullWrapped.contains("<li>"));
        assertEquals("", 7, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(plainFullWrapped)));

        final String plainMinimal = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        System.out.println("Plaintext minimal:\n[" + plainMinimal + "]");
        assertTrue("Plaintext minimal does not have title", !plainMinimal.contains("Identified problems:"));
        assertTrue("Plaintext minimal has cause", plainMinimal.contains("There was an error."));
        assertTrue("Plaintext minimal does not have indications", !plainMinimal.contains("Indication 1"));
        assertTrue("Plaintext minimal is not HTML", !plainMinimal.contains("<li>"));
        assertEquals("", 1, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(plainMinimal)));

        final String htmlFull = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, includeIndications=true}");
        System.out.println("HTML full:\n[" + htmlFull + "]");
        assertTrue("HTML full has title", htmlFull.contains("Identified problems:"));
        assertTrue("HTML full has cause", htmlFull.contains("There was an error."));
        assertTrue("HTML full has indications", htmlFull.contains("Indication 1"));
        assertTrue("HTML full is HTML", htmlFull.contains("<li>"));
        assertEquals("", 1, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(htmlFull)));

        final String htmlMinimal = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, includeIndications=false}");
        System.out.println("HTML minimal:\n[" + htmlMinimal + "]");
        assertTrue("HTML minimal does not have title", !htmlMinimal.contains("Identified problems:"));
        assertTrue("HTML minimal has cause", htmlMinimal.contains("There was an error."));
        assertTrue("HTML minimal does not have indications", !htmlMinimal.contains("Indication 1"));
        assertTrue("HTML minimal is HTML", htmlMinimal.contains("<li>"));
        assertEquals("", 1, Iterables.size(Splitter.on('\n').omitEmptyStrings().split(htmlMinimal)));
    }

    /**
     * Test that wrap() works appropriately.
     * @throws Exception if necessary
     */
    @Test
    public void testWrap() throws Exception {
        // CS IGNORE OperatorWrap FOR NEXT 11 LINES. REASON: Test data.
        final String text =
            "Lorem ipsum dolor sit amet,\n" +
            "consectetur adipiscing elit. Nulla euismod sapien ligula,\n" +
            "\n" +
            "    ac euismod quam aliquet vel.\n" +
            "    Duis quam augue, tristique in mi ac, scelerisque\n" +
            "    euismod nibh.\n" +
            "\n" +
            "Nulla accumsan velit nec neque sollicitudin,\n" +
            "eget sagittis purus vestibulum. Nunc cursus ornare sapien\n" +
            "sit amet hendrerit. Proin non nisi sapien.";
        // No additional wrapping.
        final int noWrapping = 0;
        final List<String> unwrappedLines = TokenUtils.wrap(text, noWrapping);
        System.out.println("Unwrapped lines:");
        for (final String line : unwrappedLines) {
            System.out.println(line);
        }
        final int expectedNoWrappingLineCount = 10;
        assertEquals(expectedNoWrappingLineCount, unwrappedLines.size());
        final int wrapAt35 = 35;
        final List<String> wrappedAt35 = TokenUtils.wrap(text, wrapAt35);
        System.out.println("Wrapped at 35:");
        for (final String line : wrappedAt35) {
            System.out.println(line);
        }
        final int expectedWrapAt35LineCount = 15;
        assertEquals(expectedWrapAt35LineCount, wrappedAt35.size());
    }

    /**
     * Tests the expansion when there is no failure for the default setup.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testNoFailureWithDefaultEmptyText() throws Exception {
        // CS IGNORE MagicNumberCheck FOR NEXT 7 LINES. REASON: Test data.
        final FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild noCauseBuild = noCauseBuildFuture.get(10, TimeUnit.SECONDS);
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener, "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to the empty string.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testNoFailureWithEmptyText() throws Exception {
        // CS IGNORE MagicNumberCheck FOR NEXT 8 LINES. REASON: Test data.
        final FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild noCauseBuild = noCauseBuildFuture.get(10, TimeUnit.SECONDS);
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"\"}");
        assertEquals("", defaultNoResult);
    }

    /**
     * Tests the expansion when there is no failure with noFailureText set to something.
     *
     * @throws Exception If necessary
     */
    @Test
    public void testNoFailureWithText() throws Exception {
        // CS IGNORE MagicNumberCheck FOR NEXT 8 LINES. REASON: Test data.
        final FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild noCauseBuild = noCauseBuildFuture.get(10, TimeUnit.SECONDS);
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener,
                "${BUILD_FAILURE_ANALYZER, noFailureText=\"Sample text with <b>html</b>\"}");
        assertEquals("Sample text with <b>html</b>", defaultNoResult);
    }

}
