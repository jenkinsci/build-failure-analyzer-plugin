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

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.dbf.DownstreamBuildFinder;
import hudson.matrix.MatrixRun;
import hudson.model.BuildBadgeAction;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The action to show the {@link FailureCause} to the user..
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@ExportedBean
public class FailureCauseBuildAction implements BuildBadgeAction {
    private transient List<FailureCause> failureCauses;
    private List<FoundFailureCause> foundFailureCauses;
    /**
     * The url of this action.
     */
    public static final String URL_NAME = "bfa";
    private static final Logger logger = Logger.getLogger(FailureCauseBuildAction.class.getName());

    private Run build;

    /**
     * Standard constructor.
     *
     * @param foundFailureCauses the FoundFailureCauses.
     */
    public FailureCauseBuildAction(List<FoundFailureCause> foundFailureCauses) {
        this.foundFailureCauses = foundFailureCauses;
    }

    @Override
    public String getIconFileName() {
        if (Jenkins.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (Jenkins.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)) {
            return Messages.CauseManagement_DisplayName();
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    /**
     * Getter for the FoundFailureCauses.
     *
     * @return the FoundFailureCauses.
     */
    @Exported
    public List<FoundFailureCause> getFoundFailureCauses() {
        return foundFailureCauses;
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
     * Convenience method for jelly access to PluginImpl.
     *
     * @return the PluginImpl instance.
     */
    public PluginImpl getPluginImpl() {
        return PluginImpl.getInstance();
    }

    /**
     * Called after deserialization. Converts {@link #failureCauses} if existing.
     *
     * @return this.
     */
    public Object readResolve() {
        if (failureCauses != null) {
            List<FoundFailureCause> list = new LinkedList<FoundFailureCause>();
            for (FailureCause fc : failureCauses) {
                list.add(new FoundFailureCause(fc));
            }
            foundFailureCauses = list;
            failureCauses = null;
        }
        return this;
    }

    /**
     * Used when we are directed to a FoundFailureCause beneath the build action.
     *
     * @param token the FoundFailureCause number of this build action we are trying to navigate to.
     * @param req the stapler request.
     * @param resp the stapler response.
     * @return the correct FoundFailureCause.
     */
    public FoundFailureCause getDynamic(String token, StaplerRequest req, StaplerResponse resp) {
        try {
            int causeNumber = Integer.parseInt(token) - 1;
            if (causeNumber >= 0 && causeNumber < foundFailureCauses.size()) {
                return foundFailureCauses.get(causeNumber);
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "[BFA] Failed to parse token for getDynamic: " + token);
            return null;
        }
        logger.log(Level.WARNING, "[BFA] Unable to navigate to the FailureCause: " + token);
        return null;
    }

    /**
     * Used for the link to the failure cause management page.
     *
     * @param req the stapler request.
     * @param resp the stapler response
     * @throws IOException if so.
     */
    public void doIndex(StaplerRequest req, StaplerResponse resp) throws IOException {
        resp.sendRedirect2("../../failure-cause-management");
    }

    /**
     * Setter for the build triggering this object.
     *
     * @param build - the build corresponding to this action
     */
    public void setBuild(Run build) {
        this.build = build;
    }

    /**
     * Getter for the build.
     *
     * @return the build corresponding to this action
     */
    public Run getBuild() {
        return build;
    }

    /**
     * Getter for the FailureCauseDisplayData.
     *
     * @return the FailureCauseDisplayData.
     */
    @Exported
    public FailureCauseDisplayData getFailureCauseDisplayData() {
        FailureCauseDisplayData failureCauseDisplayData
                = getDownstreamData(this, 0);

        // Fallback, if no build is stored in in build action,
        if (failureCauseDisplayData == null) {
            failureCauseDisplayData = new FailureCauseDisplayData();
            failureCauseDisplayData.setFoundFailureCauses(
                    this.getFoundFailureCauses());
        }

        return failureCauseDisplayData;
    }


    /**
     * Populates the supplied FailureCauseDisplayData with FailureCause from the
     * build and then recursively collect data from downstream builds. If
     * buildAction doesn't have a connected build null is returned.
     *
     * @param buildAction the action to retrieve data from
     * @param depth recursive depth
     * @return FailureCauseDisplayData
     */
    private static FailureCauseDisplayData getDownstreamData(
            final FailureCauseBuildAction buildAction, final int depth) {

        final int maxDepth = 10;

        FailureCauseDisplayData displayData = null;
        // Preventing us to get into a recursive loop
        if (depth < maxDepth && buildAction.getBuild() != null) {
            Run build = buildAction.getBuild();
            displayData = new FailureCauseDisplayData(build.getParent().getUrl(),
                    build.getParent().getDisplayName(),
                    build.getUrl(),
                    build.getDisplayName());

            // Add causes from this build
            displayData.setFoundFailureCauses(
                    buildAction.getFoundFailureCauses());
            for (Run run
                    : getDownstreamBuilds(buildAction.getBuild())) {

                checkSubFailureCauseBuildAction(
                        run, displayData, depth);
            }
        }
        return displayData;
    }

    /**
     * Check if the build has the action FailureCauseBuildAction. If so, add
     * information to the display data.
     *
     * @param run the build under investigation
     * @param displayData object holding display information
     * @param depth recursive depth
     */
    private static void checkSubFailureCauseBuildAction(
            final Run run,
            final FailureCauseDisplayData displayData,
            final int depth) {
        FailureCauseBuildAction subAction =
                run.getAction(FailureCauseBuildAction.class);
        if (subAction != null) {
            setSubDisplayData(subAction, displayData, depth);
        } else {
            // Nested matrix build
            FailureCauseMatrixBuildAction subMatrixAction =
                    run.getAction(
                            FailureCauseMatrixBuildAction.class);
            if (subMatrixAction != null) {
                for (MatrixRun matrixRun
                        : subMatrixAction.getRunsWithAction()) {
                    FailureCauseBuildAction action = matrixRun.getAction(
                            FailureCauseBuildAction.class);
                    if (action != null) {
                        FailureCauseDisplayData subDisplayData =
                                setSubDisplayData(action, displayData, depth);
                        adjustProjectDisplayName(run, subDisplayData);
                    }
                }
            }
        }
    }

    /**
     * Add display data from the action.
     *
     * @param subAction the action under investigation
     * @param displayData object holding display information
     * @param depth recursive depth
     * @return the added display data object
     */
    private static FailureCauseDisplayData setSubDisplayData(
            final FailureCauseBuildAction subAction,
            final FailureCauseDisplayData displayData,
            final int depth) {
        FailureCauseDisplayData subDisplayData =
                getDownstreamData(subAction, depth + 1);
        if (subDisplayData != null) {
            displayData.addDownstreamFailureCause(
                    subDisplayData);
        }
        return subDisplayData;
    }

    /**
     * A matrix project returns for each run a project display name on the form
     * PROJECT » CONFIGURATION #NBR".
     * When nested there is one link to the project and one to the build. The
     * build nbr is removed from the name.
     *
     * @param run the build generating the build failure
     * @param subDisplayData the data object to update
     */
    private static void adjustProjectDisplayName(
            final Run run,
            final FailureCauseDisplayData subDisplayData) {
        if (subDisplayData != null) {
            subDisplayData.getLinks().setProjectDisplayName(
                    run.getParent().getFullName() + " » "
                    + subDisplayData.getLinks().getProjectDisplayName());
        }
    }

    /**
     * Returns a set with downstream builds. Add more or change to alternative
     * ways of collecting downstream builds.
     *
     * @param build collect downstream builds from this
     * @return a set of downstream builds
     */
    private static Set<Run<?, ?>> getDownstreamBuilds(
            final Run build) {

        Set<Run<?, ?>> foundDbf = new TreeSet<Run<?, ?>>();

        for (DownstreamBuildFinder dbf : DownstreamBuildFinder.getAll()) {

            List<Run<?, ?>> downstreamBuilds = dbf.getDownstreamBuilds(build);

            for (Run<?, ?> downstreamBuild : downstreamBuilds) {
                if (downstreamBuild != null) {
                    foundDbf.add(downstreamBuild);
                } else {
                    logger.log(Level.FINER, "[BFA] Ignoring missing downstream build for " + build.getFullDisplayName());
                }
            }

        }
        return foundDbf;
    }
}
