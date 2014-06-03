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

package com.sonyericsson.jenkins.plugins.bfa.model;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;

/**
 * Found Failure Cause of a build.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@ExportedBean
public class FoundFailureCause {
    private static final Logger logger = Logger.getLogger(FoundFailureCause.class.getName());

    private final String id;
    private final String name;
    private final String description;
    private final List<String> categories;
    private List<FoundIndication> indications;

    /**
     * Constructor used when converting old failureCauses to foundFailureCauses.
     *
     * @param originalCause the original FailureCause.
     */
    public FoundFailureCause(final FailureCause originalCause) {
        this(originalCause, new LinkedList<FoundIndication>());
    }

    /**
     * Standard constructor.
     *
     * @param originalCause the original FailureCause.
     * @param indications the indications found that imply this cause.
     */
    public FoundFailureCause(final FailureCause originalCause, final List<FoundIndication> indications) {
        this.id = originalCause.getId();
        this.name = originalCause.getName();
        this.categories = originalCause.getCategories();
        this.indications = new LinkedList<FoundIndication>(indications);
        this.description = buildFormattedDescription(originalCause, this.indications, originalCause.getDescription());
    }

    /**
     * Getter for the id.
     *
     * @return the id.
     */
    @Exported
    public String getId() {
        return id;
    }

    /**
     * Getter for the name.
     *
     * @return the name.
     */
    @Exported
    public String getName() {
        return name;
    }

    /**
     * Getter for the description.
     *
     * @return the description.
     */
    @Exported
    public String getDescription() {
        return description;
    }

    /**
     * Getter for the categories.
     *
     * @return the categories.
     */
    @Exported
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Getter for the list of found indications.
     *
     * @return the list.
     */
    public List<FoundIndication> getIndications() {
        if (indications == null) {
            indications = new LinkedList<FoundIndication>();
        }
        return indications;
    }

    /**
     * Builds the formatted description from build log indication regular expressions.
     * @param originalCause the original cause of the FoundFailureCause
     * @param foundIndications the indications found that the FoundFailureCause
     * @param description the description to be formatted
     * @return the formatted description
     */
    private static String buildFormattedDescription(final FailureCause originalCause,
            final List<FoundIndication> foundIndications, final String description) {
        String formattedCandidate = description;
        if (!foundIndications.isEmpty()) {
            final FoundIndication firstFoundIndication = foundIndications.get(0);
            try {
                // Find the found indication in the list of indications.
                if (originalCause != null) {
                    final List<Indication> originalCauseIndications = originalCause.getIndications();
                    int expressionIndex = 0;
                    boolean found = false;
                    for (final int size = originalCauseIndications.size(); expressionIndex < size; ++expressionIndex) {
                        if (originalCauseIndications.get(expressionIndex).getPattern().pattern().equals(
                                firstFoundIndication.getPattern())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        final int expressionNumber = expressionIndex + 1;
                        formattedCandidate = convertFormat(formattedCandidate, expressionNumber);
                        // Replace the description with the values from the matched indication. e.g.,
                        // "Foo $1" becomes
                        // "Foo value captured from indication match"
                        final Pattern contentPattern = Pattern.compile(firstFoundIndication.getPattern());
                        final Matcher contentMatcher = contentPattern.matcher(firstFoundIndication.getMatchingString());
                        formattedCandidate = contentMatcher.replaceAll(formattedCandidate);
                    }
                }
            } catch (final Exception exception) {
                logger.log(Level.SEVERE, null, exception);
            }
        }
        return formattedCandidate;
    }

    /**
     * Convert "${i,G}" to "$G" and "${E,G}" to "" while ignoring "\${E,G}".
     * @param input the input string that may contain replacement tokens of the form ${E,G}, where E is the
     * expression number and G is the captured group within the expression numbered E.
     * @param expressionNumber the 1-based expression number in a list of expressions that may contain captured groups
     * @return the input string with replacement tokens replaced by {@code Matcher} group number tokens
     */
    /* package private */ static String convertFormat(final String input, final int expressionNumber) {
        // Replace the input's "${i,G}" with "$M". e.g., if i == 2,
        // "Foo ${2,1}${3,1}" becomes "Foo $1${3,1}"
        // Do not replace \${E,G}.
        final Pattern expressionPattern = Pattern.compile(
            "(?<!\\\\)\\$\\{\\s*" + Integer.toString(expressionNumber) + "\\s*,\\s*(\\d+?)\\s*\\}");
        final Matcher expressionMatcher = expressionPattern.matcher(input);
        final String expressionTokensReplaced = expressionMatcher.replaceAll("\\$$1");
        // Replace the rest of input's "${E,G}" with "". e.g.,
        // "Foo $1${3,1}" becomes "Foo $1"
        // Do not replace \${E,G}.
        final Pattern nonExpressionPattern = Pattern.compile("(?<!\\\\)\\$\\{\\s*\\d+?\\s*,\\s*\\d+?\\s*\\}");
        final Matcher nonExpressionMatcher = nonExpressionPattern.matcher(expressionTokensReplaced);
        final String nonExpressionTokensRemoved = nonExpressionMatcher.replaceAll("");
        // Because we ignored \${E,G}, now replace \${E,G} with ${E,G}.
        final Pattern escapedTokenPattern = Pattern.compile("\\\\(\\$\\{\\s*\\d+?\\s*,\\s*\\d+?\\s*\\})");
        final Matcher escapedTokenMatcher = escapedTokenPattern.matcher(nonExpressionTokensRemoved);
        return escapedTokenMatcher.replaceAll("$1");
    }
}
