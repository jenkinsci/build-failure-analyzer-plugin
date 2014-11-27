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

import com.sonyericsson.jenkins.plugins.bfa.FailureCauseMatrixAggregator;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.utils.OldDataConverter;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.BuildBadgeAction;
import hudson.model.Cause;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build action for the aggregated result of failure causes.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCauseMatrixBuildAction implements BuildBadgeAction {

    private static final Logger logger = Logger.getLogger(FailureCauseMatrixBuildAction.class.getName());
    private transient List<MatrixRun> runs;
    private List<String> runIds;
    private MatrixBuild build;

    /**
     * Standard constructor.
     *
     * @param build the build where this action is placed.
     * @param runs  the list of MatrixRuns for this action.
     */
    public FailureCauseMatrixBuildAction(MatrixBuild build, List<MatrixRun> runs) {
        this.build = build;
        this.runs = runs;
        makeIdList(runs);
    }

    /**
     * Generates the contents of {@link #runIds} so it contains some type of identifiable to later retrieve the runs
     * when this is read from disk.
     *
     * @param matrixRuns the list of runs to get the ids from.
     */
    private void makeIdList(List<MatrixRun> matrixRuns) {
        logger.finer("making runIds");
        this.runIds = new LinkedList<String>();
        for (MatrixRun run : matrixRuns) {
            MatrixConfiguration configuration = run.getProject();
            runIds.add(configuration.getCombination().toString());
        }
        logger.log(Level.FINER, "runIds size: {0}", runIds.size());
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
        for (MatrixRun run : getRuns()) {
            if (run.getAction(FailureCauseBuildAction.class) != null) {
                returnList.add(run);
            }
        }
        return returnList;
    }

    /**
     * Gets the {@link #runs}, if they haven't been loaded yet they will.
     *
     * @return the runs.
     */
    private synchronized List<MatrixRun> getRuns() {
        if (runIds != null && build != null && runs == null) {
            runs = new LinkedList<MatrixRun>();
            for (String id : runIds) {
                Combination combination = Combination.fromString(id);
                if (combination != null) {
                    MatrixRun run = build.getRun(combination);
                    if (run != null) {
                        runs.add(run);
                    }
                }
            }
        }
        return runs;
    }

    /**
     * Finds the first run with the first identified cause. Null if there are none.
     *
     * @return the first cause found.
     */
    public FoundFailureCause getFirstFailureCause() {
        for (MatrixRun run : getRuns()) {
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
        return PluginImpl.getFullImageUrl("48x48", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Gets the image url for the badge page.
     *
     * @return the image url.
     */
    public String getBadgeImageUrl() {
        return PluginImpl.getFullImageUrl("16x16", PluginImpl.DEFAULT_ICON_NAME);
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

    /**
     * Gets the failure causes for a specific matrix run.
     *
     * @param run the run to find failure causes for.
     * @return the failure causes of the run.
     */
    public static FailureCauseDisplayData getFailureCauseDisplayData(MatrixRun run) {
        FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        if (action != null) {
            return action.getFailureCauseDisplayData();
        }
        return new FailureCauseDisplayData();
    }

    /**
     * Signal that this object is de-serialized. Will start by checking if {@link #runs} should be converted, otherwise
     * check if {@link #runIds} should be converted to {@link #runs}.
     *
     * @return this object.
     */
    public Object readResolve() {
        if (needsConvertOld()) {
            String project = findUpStreamName();
            if (project != null) {
                logger.log(Level.FINE, "Scheduling a build in {0} for conversion.", project);
                OldDataConverter.getInstance().convertMatrixBuildAction(project, this);
            } else {
                logger.warning("A MatrixProject's failure cause action needs to be converted,"
                        + " but the project name could not be discovered.");
            }
        }
        return this;
    }

    /**
     * Check to see if {@link #convertOldData()} is needed.
     *
     * @return true if so.
     */
    public synchronized boolean needsConvertOld() {
        return
                (runs != null && (runIds == null || runIds.isEmpty())) //Old data not having runIds
                        || (build == null); //Need to find build
    }

    /**
     * Converts from the use of the buggy {@link #runs} to use {@link #runIds}. Only does the conversion if needed.
     */
    public synchronized void convertOldData() {
        List<MatrixRun> newRuns = null;
        if (runs != null && (runIds == null || runIds.isEmpty())) {
            logger.fine("Starting conversion");
            //old and bad stuff lets do our best to convert.
            for (MatrixRun run : runs) {
                if (build == null) {
                    build = findUpStream(run);
                    logger.log(Level.FINEST, "Build is {0}", build);
                }
                if (build != null) {
                    logger.finer("Found a build.");
                    newRuns = FailureCauseMatrixAggregator.getRuns(build);
                    makeIdList(newRuns);
                    break;
                }
            }
        }

        if (newRuns != null) {
            logger.finer("Setting runs");
            runs = newRuns;
        }
        logger.exiting("FailureCauseMatrixBuildAction", "convertOldData");
    }

    /**
     * Finds the name of the matrix project that this action probably belongs to.
     * @return the name of the project or null if runs are bad.
     */
    public synchronized String findUpStreamName() {
        if (runs != null) {
            for (MatrixRun run : runs) {
                Cause.UpstreamCause cause = run.getCause(Cause.UpstreamCause.class);
                if (cause != null) {
                    return cause.getUpstreamProject();
                }
            }
        }
        return null;
    }

    /**
     * Helper method for {@link #readResolve()}, will try to find the upstream {@link MatrixBuild}. Since the run is
     * de-serialized badly (not referenced in the correct hierarchy) this could be a bit tricky.
     *
     * @param run the MatrixRun to search.
     * @return the correct instance if possible.
     */
    private MatrixBuild findUpStream(MatrixRun run) {
        Cause.UpstreamCause cause = run.getCause(Cause.UpstreamCause.class);
        if (cause != null) {
            String project = cause.getUpstreamProject();
            //Yes the folders plugin could cause problems here,
            // but stapler can't help me if I use the URL which would be preferred.
            //TODO what to do when the job is renamed?
            TopLevelItem item = null;
            item = Jenkins.getInstance().getItem(project);
            logger.log(Level.FINE, "Project item for {0} is {1}", new Object[]{project, item});
            if (item != null && item instanceof MatrixProject) {
                logger.log(Level.FINEST, "It is a matrix project; searching for build {0}", cause.getUpstreamBuild());
                //Find the build
                return ((MatrixProject)item).getBuildByNumber(cause.getUpstreamBuild());
            }
        }
        return null;
    }
}
