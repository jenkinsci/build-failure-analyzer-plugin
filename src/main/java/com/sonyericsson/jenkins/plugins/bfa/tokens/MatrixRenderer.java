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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.matrix.MatrixRun;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

/**
 * Renders the content of the TokenMacro. This is a split off of Token and allows to render without using the Token,
 * for example from inside a pipeline.
 */
public class MatrixRenderer extends Renderer {

    public String render(final FailureCauseMatrixBuildAction matrixAction) {

        final StringBuilder stringBuilder = new StringBuilder();
        addTitle(stringBuilder);
        final int indentLevel = 0;
        addFailureCauseMatrixRepresentation(stringBuilder, matrixAction, indentLevel);
        return stringBuilder.toString();
    }

    /**
     * @param stringBuilder the string builder to which to add the matrix build representation
     * @param matrixAction  the matrix action
     * @param indentLevel   the indent level
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
     * @param matrixRun     the matrix run
     * @param indentLevel   the indent level
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
}
