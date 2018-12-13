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

import com.google.common.collect.ListMultimap;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandTask;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.Map;

/**
 * The Build Failure Analyzer token for TokenMacro consumers.
 * @author K. R. Walker &lt;krwalker@stellarscience.com&gt;
 */
@Extension(optional = true)
public class Token extends DataBoundTokenMacro {

    private Renderer renderer = new Renderer();

    /**
     * @param includeIndications When true, the indication numbers and links into the console log are included
     * in the token replacement text.
     */
    @Parameter
    public void setIncludeIndications(final boolean includeIndications) {
        this.renderer.setIncludeIndications(includeIndications);
    }

    /**
     * @param useHtmlFormat When true, the replacement text will be an HTML snippet.
     */
    @Parameter
    public void setUseHtmlFormat(final boolean useHtmlFormat) {
        this.renderer.setUseHtmlFormat(useHtmlFormat);
    }

    /**
     * @param includeTitle When true, the title will appear in the token replacement text.
     */
    @Parameter
    public void setIncludeTitle(final boolean includeTitle) {
        this.renderer.setIncludeTitle(includeTitle);
    }

    /**
     * @param wrapWidth Wrap long lines at this width. If wrapWidth is 0, the text isn't wrapped. Only applies if
     * useHtmlFormat == false.
     */
    @Parameter
    public void setWrapWidth(final int wrapWidth) {
        this.renderer.setWrapWidth(wrapWidth);
    }

    /**
     * @param noFailureText Text to return when no failure cause is present.
     */
    @Parameter
    public void setNoFailureText(final String noFailureText) {
        this.renderer.setNoFailureText(noFailureText);
    }

    @Override
    public boolean acceptsMacroName(final String macroName) {
        return "BUILD_FAILURE_ANALYZER".equals(macroName);
    }

    @Override
    public String evaluate(final AbstractBuild<?, ?> build, final TaskListener listener, final String macroName)
        throws MacroEvaluationException, IOException, InterruptedException {

        return evaluate(build);
    }

    @Override
    public String evaluate(final Run<?, ?> run, final FilePath workspace, final TaskListener listener,
                           final String macroName, final Map<String, String> arguments,
                           final ListMultimap<String, String> argumentMultimap)
            throws MacroEvaluationException, IOException, InterruptedException {

        return evaluate(run);
    }

    /**
     * @param run The run to analyze
     * @return The results of the build failure analyzer
     */
    private String evaluate(final Run<?, ?> run) {

        // Scan the build now.
        new ScanOnDemandTask(run).run();

        final FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        if (action != null) {
            return renderer.render(action);
        }

        final FailureCauseMatrixBuildAction matrixAction = run.getAction(FailureCauseMatrixBuildAction.class);
        if (matrixAction != null) {
            return renderer.render(matrixAction);
        }

        // If there are no found failures, return an empty string.
        return "";
    }
}
