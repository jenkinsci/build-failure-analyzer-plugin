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
    private static final int ITEM_INCREMENT = 0;

    private static final int LIST_INCREMENT = 1;

    private static final String LIST_BULLET = "* ";

    private static final String LIST_BULLET_SPACE = "  ";

    private static final Logger logger = Logger.getLogger(Token.class.getName());

    /**
     * When true, the indication numbers and links into the console log are included in the token replacement text.
     */
    private boolean includeIndications = true;

    /**
     * When true, the replacement will be an HTML snippet.
     */
    private boolean useHtmlFormat = false;

    /**
     * When true, the "Identified problems:" title will appear over the causes.
     */
    private boolean includeTitle = true;

    /**
     * Wrap long lines at this width.If wrapWidth is 0,the text isn't wrapped. Only applies if useHtmlFormat == false.
     */
    private int wrapWidth = 0;

    /**
     * Default text to include if no problem was found. It defaults to an empty string.
     */
    private String noFailureText = "";

    /**
     * @param includeIndications When true, the indication numbers and links into the console log are included
     * in the token replacement text.
     */
    @Parameter
    public void setIncludeIndications(final boolean includeIndications) {
        this.includeIndications = includeIndications;
    }

    /**
     * @param useHtmlFormat When true, the replacement text will be an HTML snippet.
     */
    @Parameter
    public void setUseHtmlFormat(final boolean useHtmlFormat) {
        this.useHtmlFormat = useHtmlFormat;
    }

    /**
     * @param includeTitle When true, the title will appear in the token replacement text.
     */
    @Parameter
    public void setIncludeTitle(final boolean includeTitle) {
        this.includeTitle = includeTitle;
    }

    /**
     * @param wrapWidth Wrap long lines at this width. If wrapWidth is 0, the text isn't wrapped. Only applies if
     * useHtmlFormat == false.
     */
    @Parameter
    public void setWrapWidth(final int wrapWidth) {
        this.wrapWidth = wrapWidth;
    }

    /**
     * @param noFailureText Text to return when no failure cause is present.
     */
    @Parameter
    public void setNoFailureText(final String noFailureText) {
        this.noFailureText = noFailureText;
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
            final FailureCauseDisplayData data = action.getFailureCauseDisplayData();
            if (data.getFoundFailureCauses().isEmpty() && data.getDownstreamFailureCauses().isEmpty()) {
                logger.info("there were no causes");
                return noFailureText;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            addTitle(stringBuilder);
            final int indentLevel = 0;
            addFailureCauseDisplayDataRepresentation(stringBuilder, data, indentLevel);
            return stringBuilder.toString();
        }

        final FailureCauseMatrixBuildAction matrixAction = build.getAction(FailureCauseMatrixBuildAction.class);
        if (matrixAction != null) {
            final StringBuilder stringBuilder = new StringBuilder();
            addTitle(stringBuilder);
            final int indentLevel = 0;
            addFailureCauseMatrixRepresentation(stringBuilder, matrixAction, indentLevel);
            return stringBuilder.toString();
        }

        // If there are no found failures, return an empty string.
        return "";
    }

    /**
     * Add the "Identified problems:" title to the output.
     * @param stringBuilder the string builder to which to add the title
     */
    private void addTitle(final StringBuilder stringBuilder) {
        if (includeTitle) {
            final String title = "Identified problems:";
            if (useHtmlFormat) {
                stringBuilder.append("<h2>");
                stringBuilder.append(title);
                stringBuilder.append("</h2>");
            } else {
                stringBuilder.append(title);
                stringBuilder.append("\n");
            }
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the matrix build representation
     * @param matrixAction the matrix action
     * @param indentLevel the indent level
     */
    private void addFailureCauseMatrixRepresentation(final StringBuilder stringBuilder,
        final FailureCauseMatrixBuildAction matrixAction, final int indentLevel) {

        final List<MatrixRun> matrixRuns = matrixAction.getRunsWithAction();
        if (useHtmlFormat) {
            stringBuilder.append("<ul>");
        }
        for (final MatrixRun matrixRun : matrixRuns) {
            addMatrixRunRepresentation(stringBuilder, matrixRun, indentLevel + ITEM_INCREMENT);
        }
        if (useHtmlFormat) {
            stringBuilder.append("</ul>");
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the matrix run representation
     * @param matrixRun the matrix run
     * @param indentLevel the indent level
     */
    private void addMatrixRunRepresentation(final StringBuilder stringBuilder, final MatrixRun matrixRun,
        final int indentLevel) {

        final FailureCauseDisplayData data = FailureCauseMatrixBuildAction.getFailureCauseDisplayData(matrixRun);
        if (data.getFoundFailureCauses().isEmpty() && data.getDownstreamFailureCauses().isEmpty()) {
            return;
        }
        final int nextIndentLevel = indentLevel + LIST_INCREMENT;
        if (useHtmlFormat) {
            stringBuilder.append("<li>");
            try {
                stringBuilder.append(Jenkins.getInstance().getMarkupFormatter().translate(
                    matrixRun.getFullDisplayName()));
            } catch (final IOException exception) {
                stringBuilder.append("matrix-full-display-name");
            }
            addFailureCauseDisplayDataRepresentation(stringBuilder, data, nextIndentLevel);
            stringBuilder.append("</li>");
        } else {
            stringBuilder.append(indentForDepth(indentLevel));
            stringBuilder.append(LIST_BULLET);
            stringBuilder.append(matrixRun.getFullDisplayName());
            stringBuilder.append("\n");
            addFailureCauseDisplayDataRepresentation(stringBuilder, data, nextIndentLevel);
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the failure cause data representation
     * @param data the failure cause display data
     * @param indentLevel the indent level
     */
    private void addFailureCauseDisplayDataRepresentation(final StringBuilder stringBuilder,
        final FailureCauseDisplayData data, final int indentLevel) {

        final IndicationUrlBuilder indicationUrlBuilder = new IndicationUrlBuilder();
        indicationUrlBuilder.setBuildUrl(data.getLinks().getBuildUrl());
        final List<FoundFailureCause> causes = data.getFoundFailureCauses();
        final int nextIndentLevel = indentLevel + ITEM_INCREMENT;
        if (useHtmlFormat) {
            stringBuilder.append("<ul>");
            for (final FoundFailureCause cause : causes) {
                indicationUrlBuilder.setCause(cause);
                addFailureCauseRepresentation(stringBuilder, indicationUrlBuilder, cause, nextIndentLevel);
            }
            stringBuilder.append("</ul>");
        } else {
            for (final FoundFailureCause cause : causes) {
                indicationUrlBuilder.setCause(cause);
                addFailureCauseRepresentation(stringBuilder, indicationUrlBuilder, cause, nextIndentLevel);
            }
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the failure cause representation
     * @param indicationUrlBuilder the indication URL builder
     * @param cause the found failure cause
     * @param indentLevel the indent level
     */
    private void addFailureCauseRepresentation(final StringBuilder stringBuilder,
        final IndicationUrlBuilder indicationUrlBuilder, final FoundFailureCause cause,
        final int indentLevel) {

        final int nextIndentLevel = indentLevel + LIST_INCREMENT;
        if (useHtmlFormat) {
            stringBuilder.append("<li>");
            try {
                stringBuilder.append(Jenkins.getInstance().getMarkupFormatter().translate(cause.getName()));
            } catch (final IOException exception) {
                stringBuilder.append("cause-name");
            }
            stringBuilder.append(": ");
            try {
                stringBuilder.append(Jenkins.getInstance().getMarkupFormatter().translate(cause.getDescription()));
            } catch (final IOException exception) {
                stringBuilder.append("cause-description");
            }
            if (includeIndications) {
                addIndicationsRepresentation(stringBuilder, indicationUrlBuilder, cause.getIndications(),
                    nextIndentLevel);
            }
            stringBuilder.append("</li>");
        } else {
            // e.g.,
            //   * cause-name: cause-description that
            //     can wrap lines
            // |-------------------------------------|
            // AA
            //   BB
            // CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC
            //
            // A = indentForDepth()
            // B = LIST_BULLET.length()
            // C = wrapWidth
            final List<String> lines = wrap(
                cause.getName() + ": " + cause.getDescription(),
                // C - A - B
                wrapWidth - indentForDepth(indentLevel).length() - LIST_BULLET.length());
            for (int lineIndex = 0, lineCount = lines.size(); lineIndex < lineCount; ++lineIndex) {
                if (lineIndex == 0) {
                    stringBuilder.append(LIST_BULLET);
                } else {
                    stringBuilder.append(LIST_BULLET_SPACE);
                }
                stringBuilder.append(lines.get(lineIndex));
                stringBuilder.append("\n");
            }
            if (includeIndications) {
                addIndicationsRepresentation(stringBuilder, indicationUrlBuilder, cause.getIndications(),
                    nextIndentLevel);
            }
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the indication list representation
     * @param indicationUrlBuilder the indication URL builder
     * @param indications the indication list
     * @param indentLevel the indent level
     */
    private void addIndicationsRepresentation(final StringBuilder stringBuilder,
        final IndicationUrlBuilder indicationUrlBuilder, final List<FoundIndication> indications,
        final int indentLevel) {

        final int nextIndentLevel = indentLevel + ITEM_INCREMENT;
        if (useHtmlFormat) {
            stringBuilder.append("<ul>");
            for (int i = 0, size = indications.size(); i < size; ++i) {
                final FoundIndication indication = indications.get(i);
                indicationUrlBuilder.setIndication(indication);
                final int indicationNumber = i + 1;
                addIndicationRepresentation(stringBuilder, indicationUrlBuilder, indication, indicationNumber,
                    nextIndentLevel);
            }
            stringBuilder.append("</ul>");
        } else {
            for (int i = 0, size = indications.size(); i < size; ++i) {
                final FoundIndication indication = indications.get(i);
                indicationUrlBuilder.setIndication(indication);
                final int indicationNumber = i + 1;
                addIndicationRepresentation(stringBuilder, indicationUrlBuilder, indication, indicationNumber,
                    nextIndentLevel);
            }
        }
    }

    /**
     * @param stringBuilder the string builder to which to add the indication representation
     * @param indicationUrlBuilder the indication URL builder
     * @param indication the found indication
     * @param indicationNumber the found indication number (index+1) in the list of indications
     * @param indentLevel the indent level
     */
    private void addIndicationRepresentation(final StringBuilder stringBuilder,
        final IndicationUrlBuilder indicationUrlBuilder, final FoundIndication indication,
        final int indicationNumber, final int indentLevel) {

        if (useHtmlFormat) {
            stringBuilder.append("<li><a href=\"");
            stringBuilder.append(indicationUrlBuilder.getUrlString());
            stringBuilder.append("\">");
            stringBuilder.append("Indication ");
            stringBuilder.append(indicationNumber);
            stringBuilder.append("</a></li>");
        } else {
            stringBuilder.append(indentForDepth(indentLevel));
            stringBuilder.append(LIST_BULLET);
            stringBuilder.append("Indication ");
            stringBuilder.append(indicationNumber);
            stringBuilder.append(":\n");
            stringBuilder.append(indentForDepth(indentLevel));
            stringBuilder.append(LIST_BULLET_SPACE);
            stringBuilder.append("<");
            stringBuilder.append(indicationUrlBuilder.getUrlString());
            stringBuilder.append(">\n");
        }
    }

    /**
     * @param indentLevel the indent level
     * @return a whitespace string with an appropriate with for the specified indent level
     */
    private static String indentForDepth(final int indentLevel) {
        return StringUtils.repeat("  ", indentLevel);
    }

    /**
     * Wrap some text
     * @param text some text to wrap
     * @param width the text will be wrapped to this many characters
     * @return the text lines
     */
    /* package private */ static List<String> wrap(final String text, final int width) {
        final List<String> lines = new ArrayList< String>();
        final Splitter lineSplitter = Splitter.on(Pattern.compile("\\r?\\n"));
        //Split the text into lines
        for (final String line : lineSplitter.split(text)) {
            if (width > 0) {
                final Pattern firstNonwhitespacePattern = Pattern.compile("[^\\s]");
                final Matcher firstNonwhiteSpaceMatcher = firstNonwhitespacePattern.matcher(line);
                String indent = "";
                if (firstNonwhiteSpaceMatcher.find()) {
                    indent = line.substring(0, firstNonwhiteSpaceMatcher.start());
                }
                //Wrap each line
                final String wrappedLines = WordUtils.wrap(line, width - indent.length());
                //Split the wrapped line into lines and add those lines to the result
                for (final String wrappedLine : lineSplitter.split(wrappedLines)) {
                    lines.add(indent + wrappedLine.trim());
                }
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Helps build a URL into the build log for an indication.
     */
    private static class IndicationUrlBuilder {
        private String buildUrl = "";

        private String causeId = "";

        private String indicationHash = "";

        /**
         * @param buildUrl the url for the indication's build
         */
        void setBuildUrl(final String buildUrl) {
            this.buildUrl = buildUrl;
        }

        /**
         * @param cause the cause containing the indication
         */
        void setCause(final FoundFailureCause cause) {
            this.causeId = cause.getId();
        }

        /**
         * @param indication the indication
         */
        void setIndication(final FoundIndication indication) {
            this.indicationHash = String.valueOf(indication.getMatchingHash());
        }

        /**
         * @return the string representation of the URL into the build log for the indication
         */
        String getUrlString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(Jenkins.getInstance().getRootUrl());
            builder.append("/");
            builder.append(buildUrl);
            builder.append("consoleFull#");
            builder.append(indicationHash);
            builder.append(causeId);
            return builder.toString();
        }

        @Override
        public String toString() {
            return getUrlString();
        }
    }
}
