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
package com.sonyericsson.jenkins.plugins.bfa.sod;

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.Iterators;
import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandBaseAction.ScanMode.BFA_SOD_BUILD_TYPE;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Action class for scanning non scanned build.
 *
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandBaseAction implements Action {

    /** The project. */
    private Job project;

    /**
     * SODBaseAction constructor.
     *
     * @param project current project.
     */
    public ScanOnDemandBaseAction(final Job project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        if (hasPermission()) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (hasPermission()) {
            return Messages.FailureScan_DisplayName();
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        if (hasPermission()) {
            return "scan-on-demand";
        } else {
            return null;
        }
    }

    /**
     * The full url to this action instance.
     *
     * @return the full url
     * @see Job#getUrl()
     */
    @CheckForNull
    private String getFullUrl() {
        if (getUrlName() == null) {
            return null; //No permission
        }
        return Functions.joinPath(project.getUrl(), getUrlName());
    }


    /**
     * Checks if the current user has {@link Item#CONFIGURE} or {@link Project#BUILD} permission.
     *
     * @return true if so.
     */
    public boolean hasPermission() {
        return project.hasPermission(Item.CONFIGURE) || project.hasPermission(Project.BUILD);
    }

    /**
     * Checks if the current user has {@link Item#CONFIGURE} or {@link Project#BUILD} permission.
     *
     * @see #hasPermission()
     * @see hudson.security.ACL#checkPermission(hudson.security.Permission)
     */
    public void checkPermission() {
        if (!hasPermission()) {
            throw new AccessDeniedException(
                    Messages.SodAccessDeniedException(Jenkins.getAuthentication().getName(),
                            Item.CONFIGURE.name, Project.BUILD.name));
        }
    }

    /**
     * Returns the project.
     *
     * @return project
     */
    public final Job<?, ?> getProject() {
        return project;
    }

    /**
     * Method for remove matrix run actions.
     *
     * @param build the MatrixBuild.
     */
    public void removeRunActions(MatrixBuild build) {
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            if (run.getNumber() == build.getNumber()) {
                FailureCauseBuildAction fcba = run.getAction(FailureCauseBuildAction.class);
                if (fcba != null) {
                    run.getActions().remove(fcba);
                }
                FailureCauseMatrixBuildAction fcmba = run.getAction(FailureCauseMatrixBuildAction.class);
                if (fcmba != null) {
                    run.getActions().remove(fcmba);
                }
            }
        }
    }

    /**
     * Shortcut method to
     * {@link #getDefault()}.{@link ScanMode#doPerformScan(ScanOnDemandBaseAction, StaplerRequest, StaplerResponse)}
     * If the user clicks on scan on the default scanmode page.
     *
     * @param request the request
     * @param response the response
     * @throws ServletException if so
     * @throws InterruptedException if so
     * @throws IOException if so
     */
    public void doPerformScan(StaplerRequest request, StaplerResponse response)
            throws ServletException, InterruptedException, IOException {
        getDefault().doPerformScan(this, request, response);
    }

    /**
     * Finds the user's default {@link ScanMode}.
     * If no selection is found in the session, or not in the request scope then {@link NonScanned} is returned.
     *
     * @return the default mode.
     */
    public ScanMode getDefault() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            String selected = (String)request.getSession(true).getAttribute(BFA_SOD_BUILD_TYPE);
            if (!isBlank(selected)) {
                ScanMode mode = getMode(selected);
                if (mode != null) {
                    return mode;
                }
            }
        }
        return getMode(NonScanned.URL);
    }

    /**
     * Stapler function to enable 'scan-on-demand/all' etc.
     *
     * @param url the scan mode
     *
     * @return most likely a {@link ScanMode}
     */
    @SuppressWarnings("unused") //Stapler function
    public Object getDynamic(String url) {
        if (isBlank(url)) {
            return getDefault();
        } else {
            return getMode(url);
        }
    }

    /**
     * Finds the mode with the provided url as returned by {@link ScanMode#getUrlName()}.
     *
     * @param url the url to match
     *
     * @return the scan mode or null if no matching scan mode is found.
     */
    public ScanMode getMode(String url) {
        for (ScanMode mode : ExtensionList.lookup(ScanMode.class)) {
            if (mode.getUrlName().equals(url)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * Represents the different scan modes that can be used to re-scan the builds of a Job.
     *
     * @see NonScanned
     * @see AllBuilds
     */
    @Restricted(NoExternalUse.class)
    public abstract static class ScanMode implements ExtensionPoint {

        /**
         * Session key used to store the default scan mode
         */
        static final String BFA_SOD_BUILD_TYPE = "bfa-sod-buildType";

        /**
         * If there is any run in the job matching this scan mode's criteria.
         * Default implementation is {@link #getRuns(Job)}{@code .hasNext()}
         * @param job the job to check
         *
         * @return true if so.
         */
        @SuppressWarnings("unused") //Called by the view
        public boolean hasAnyRun(Job job) {
            return getRuns(job).hasNext();
        }

        /**
         * The short relative url name of this scan mode.
         * Also used as a short identifier of the scan mode.
         *
         * @return the url name
         */
        @Nonnull
        public abstract String getUrlName();

        /**
         * The full url from the root of Jenkins.
         *
         * @return the full url
         * @see ScanOnDemandBaseAction#getFullUrl()
         */
        @CheckForNull
        public String getFullUrl() {
            final String fullUrl = getParent().getFullUrl();
            if (fullUrl == null) {
                return null;
            }
            return Functions.joinPath(fullUrl, getUrlName());
        }

        /**
         * Human readable name to display.
         *
         * @return the name
         */
        @Nonnull
        public abstract String getDisplayName();

        /**
         * Provides an iterator of the {@link Run}s of the provided job that matches this scan mode.
         *
         * @param job the job to filter builds from
         * @return an iterator of the matching builds
         */
        @Nonnull
        abstract Iterator<Run> getRuns(Job job);

        /**
         * Sets this scan mode as the default for this user on future page visits.
         */
        @SuppressWarnings("unused") //Called by the view
        public void setAsDefault() {
            StaplerRequest request = Stapler.getCurrentRequest();
            if (request == null) {
                throw new IllegalStateException("setAsDefault() can only be called in request scope");
            }
            request.getSession(true).setAttribute(BFA_SOD_BUILD_TYPE, getUrlName());
        }

        /**
         * Finds the {@link ScanOnDemandBaseAction} in the ancestor path.
         *
         * Throws {@link IllegalStateException} if we are not in the request scope and
         * {@link Stapler#getCurrentRequest()} returns null.
         *
         * @return the ancestor action
         *
         */
        @Nonnull
        public ScanOnDemandBaseAction getParent() {
            StaplerRequest request = Stapler.getCurrentRequest();
            if (request == null) {
                throw new IllegalStateException("getParent can only be called from request scope.");
            }
            Ancestor ancestor = request.findAncestor(ScanOnDemandBaseAction.class);
            if (ancestor == null) {
                throw new IllegalStateException("Not within the path of ScanOnDemandBaseAction");
            }
            final ScanOnDemandBaseAction object = (ScanOnDemandBaseAction)ancestor.getObject();
            if (object == null) {
                throw new IllegalStateException("Not within the path of ScanOnDemandBaseAction");
            } else {
                return object;
            }
        }

        /**
         * Submit method for running build scan.
         *
         * @param action the action we have as an ancestor
         * @param request  StaplerRequest
         * @param response StaplerResponse
         * @throws ServletException if something unfortunate happens.
         * @throws IOException if something unfortunate happens.
         * @throws InterruptedException if something unfortunate happens.
         */
        @SuppressWarnings("unused") //Called by the view
        public void doPerformScan(@AncestorInPath ScanOnDemandBaseAction action,
                                  StaplerRequest request, StaplerResponse response)
                throws ServletException, IOException, InterruptedException {
            action.checkPermission();
            Iterator<Run> runIterator = getRuns(action.getProject());
            while (runIterator.hasNext()) {
                Run run = runIterator.next();
                FailureCauseBuildAction fcba = run.getAction(FailureCauseBuildAction.class);
                if (fcba != null) {
                    run.getActions().remove(fcba); //TODO Replace instead
                }
                FailureCauseMatrixBuildAction fcmba = run.getAction(FailureCauseMatrixBuildAction.class);
                if (run instanceof MatrixBuild
                        && fcmba != null) {
                    run.getActions().remove(fcmba); //TODO Replace instead
                    action.removeRunActions((MatrixBuild)run);
                }
                ScanOnDemandTask task = new ScanOnDemandTask(run);
                ScanOnDemandQueue.queue(task);
            }
            response.sendRedirect2(Functions.joinPath("/", request.getContextPath(), getParent().getProject().getUrl()));
        }

        /**
         * Provides the lookup list of all registered {@link ScanMode}s.
         *
         * @return the list of available modes.
         */
        public static List<ScanMode> all() {
            return ExtensionList.lookup(ScanMode.class);
        }
    }


    /**
     * ScanMode that scans only previously non scanned builds.
     */
    @Extension
    @Restricted(NoExternalUse.class)
    public static class NonScanned extends ScanMode {

        /**
         * The {@link #getUrlName()} of this ScanMode.
         */
        static final String URL = "nonscanned";

        @Nonnull
        @Override
        public String getUrlName() {
            return URL;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ScanOnDemandBaseAction_NonScanned_DisplayName();
        }

        @Nonnull
        @Override
        Iterator<Run> getRuns(Job job) {
            return new Iterators.FilterIterator<Run>(job.getBuilds().iterator()) {
                @Override
                protected boolean filter(Run run) {
                    final Result result = run.getResult();
                    return result != null
                            && PluginImpl.needToAnalyze(result)
                            && run.getActions(FailureCauseBuildAction.class).isEmpty()
                            && run.getActions(FailureCauseMatrixBuildAction.class).isEmpty();
                }
            };
        }
    }

    /**
     * ScanMode that re-scans all builds regardless if they have been scanned before or not.
     */
    @Extension
    @Restricted(NoExternalUse.class)
    public static class AllBuilds extends ScanMode {
        @Nonnull
        @Override
        public String getUrlName() {
            return "all";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ScanOnDemandBaseAction_AllBuilds_DisplayName();
        }

        @Nonnull
        @Override
        Iterator<Run> getRuns(Job job) {
            return new Iterators.FilterIterator<Run>(job.getBuilds().iterator()) {
                @Override
                protected boolean filter(Run run) {
                    final Result result = run.getResult();
                    return result != null
                            && PluginImpl.needToAnalyze(result);
                }
            };
        }
    }
}
