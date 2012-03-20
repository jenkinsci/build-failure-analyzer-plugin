/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import hudson.matrix.MatrixRun;
import hudson.model.BuildBadgeAction;

import java.util.LinkedList;
import java.util.List;

/**
 * Build action for the aggregated result of failure causes.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCauseMatrixBuildAction implements BuildBadgeAction {

    private List<MatrixRun> runs;

    /**
     * Standard constructor.
     *
     * @param runs the list of MatrixRuns for this action.
     */
    public FailureCauseMatrixBuildAction(List<MatrixRun> runs) {
        this.runs = runs;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    /**
     * Convenience method for getting the action for a specific run.
     *
     * @param run the run to get the action for.
     * @return the FailureCauseBuildAction.
     */
    public FailureCauseBuildAction getActionForBuild(MatrixRun run) {
        return run.getAction(FailureCauseBuildAction.class);
    }

    /**
     * Gets all the matrix runs that have the failure cause build action.
     *
     * @return the runs with the action.
     */
    public List<MatrixRun> getRunsWithAction() {
        List<MatrixRun> returnList = new LinkedList<MatrixRun>();
        for (MatrixRun run : runs) {
            if (run.getAction(FailureCauseBuildAction.class) != null) {
                returnList.add(run);
            }
        }
        return returnList;
    }

    /**
     * Finds the first run with the first identified cause. Null if there are none.
     *
     * @return the first cause found.
     */
    public FoundFailureCause getFirstFailureCause() {
        for (MatrixRun run : runs) {
            FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
            if (action != null) {
                List<FoundFailureCause> foundFailureCauses = action.getFoundFailureCauses();
                if (foundFailureCauses != null && !foundFailureCauses.isEmpty()) {
                    return foundFailureCauses.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Gets the image url for the summary page.
     *
     * @return the image url.
     */
    public String getImageUrl() {
        return PluginImpl.getImageUrl("48x48", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Gets the image url for the badge page.
     *
     * @return the image url.
     */
    public String getBadgeImageUrl() {
        return PluginImpl.getImageUrl("16x16", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Gets the failure causes for a specific matrix run.
     *
     * @param run the run to find failure causes for.
     * @return the failure causes of the run.
     */
    public List<FoundFailureCause> getFoundFailureCauses(MatrixRun run) {
        FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        if (action != null) {
            return action.getFoundFailureCauses();
        }
        return new LinkedList<FoundFailureCause>();
    }
}
