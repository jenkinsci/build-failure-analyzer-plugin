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
