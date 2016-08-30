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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

/**
 * The Build Failure Analyzer token for TokenMacro consumers.
 * @author K. R. Walker &lt;krwalker@stellarscience.com&gt;
 */
@Extension(optional = true)
public class Token extends DataBoundTokenMacro {

    private MatrixRenderer renderer = new MatrixRenderer();

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

        // Scan the build now.
        new ScanOnDemandTask(build).run();

        final FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        if (action != null) {
            return renderer.render(action);
        }

        final FailureCauseMatrixBuildAction matrixAction = build.getAction(FailureCauseMatrixBuildAction.class);
        if (matrixAction != null) {
            return renderer.render(matrixAction);
        }

        // If there are no found failures, return an empty string.
        return "";
    }
}
