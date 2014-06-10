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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        final FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new PrintToLogBuilder(ERROR));
        project.getBuildersList().add(new MockBuilder(Result.FAILURE));
        final Future<FreeStyleBuild> noCauseBuildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild noCauseBuild = noCauseBuildFuture.get(10, TimeUnit.SECONDS);
        final String defaultNoResult = TokenMacro.expandAll(noCauseBuild, listener, "${BUILD_FAILURE_ANALYZER}");
        assertEquals("", defaultNoResult);
        BuildFailureScannerHudsonTest.configureCauseAndIndication("error", "There was an error.", "category",
            new BuildLogIndication(".*ERROR.*"));
        final Future<FreeStyleBuild> buildFuture = project.scheduleBuild2(0);
        final FreeStyleBuild build = buildFuture.get(10, TimeUnit.SECONDS);
        final String defaults = TokenMacro.expandAll(build, listener, "${BUILD_FAILURE_ANALYZER}");
        assertTrue("Default has title", defaults.contains("Identified problems:"));
        assertTrue("Default has cause", defaults.contains("There was an error."));
        assertTrue("Default has indications", defaults.contains("Indication 1"));
        assertTrue("Default is not HTML", !defaults.contains("<li>"));

        final String plainFull = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=true, includeIndications=true}");
        assertTrue("Default has title", plainFull.contains("Identified problems:"));
        assertTrue("Default has cause", plainFull.contains("There was an error."));
        assertTrue("Default has indications", plainFull.contains("Indication 1"));
        assertTrue("Default is not HTML", !plainFull.contains("<li>"));

        final String plainMinimal = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, includeTitle=false, includeIndications=false}");
        assertTrue("Default has title", !plainMinimal.contains("Identified problems:"));
        assertTrue("Default has cause", plainMinimal.contains("There was an error."));
        assertTrue("Default has indications", !plainMinimal.contains("Indication 1"));
        assertTrue("Default is not HTML", !plainMinimal.contains("<li>"));

        final String htmlFull = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=true, includeIndications=true}");
        assertTrue("Default has title", htmlFull.contains("Identified problems:"));
        assertTrue("Default has cause", htmlFull.contains("There was an error."));
        assertTrue("Default has indications", htmlFull.contains("Indication 1"));
        assertTrue("Default is HTML", htmlFull.contains("<li>"));

        final String htmlMiminal = TokenMacro.expandAll(build, listener,
            "${BUILD_FAILURE_ANALYZER, useHtmlFormat=true, includeTitle=false, includeIndications=false}");
        assertTrue("Default has title", !htmlMiminal.contains("Identified problems:"));
        assertTrue("Default has cause", htmlMiminal.contains("There was an error."));
        assertTrue("Default has indications", !htmlMiminal.contains("Indication 1"));
        assertTrue("Default is HTML", htmlMiminal.contains("<li>"));
    }
}
