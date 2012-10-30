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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action class for scanning non scanned build.
 *
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandBaseAction implements Action {

    /** The project. */
    private AbstractProject project;

    /**
     * SODBaseAction constructor.
     *
     * @param project current project.
     */
    public ScanOnDemandBaseAction(final AbstractProject project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return PluginImpl.getDefaultIcon();
    }

    @Override
    public String getDisplayName() {
        return Messages.FailureScan_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "scan-on-demand";
    }

    /**
     * Returns the project.
     *
     * @return project
     */
    public final AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * Method for finding sodbuilds.
     *
     * @return sodbuilds.
     */
    public List<AbstractBuild> getNotScannedBuilds() {
        List<AbstractBuild> sodbuilds = new ArrayList<AbstractBuild>();
        if (project != null) {
            List<AbstractBuild> builds = project.getBuilds();
            for (AbstractBuild build : builds) {
                if (build.getActions(FailureCauseBuildAction.class).isEmpty()
                        && build.getActions(FailureCauseMatrixBuildAction.class).isEmpty()
                        && build.getResult().isWorseThan(Result.SUCCESS)) {
                    sodbuilds.add(build);
                }
            }
        }
        return sodbuilds;
    }

    /**
     * Submit method for running build scan.
     *
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws ServletException if something unfortunate happens.
     * @throws IOException if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void doPerformScan(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException, InterruptedException {
        List<AbstractBuild> sodbuilds = getNotScannedBuilds();
        if (sodbuilds.size() > 0) {
            for (AbstractBuild sodbuild : sodbuilds) {
                ScanOnDemandTask task = new ScanOnDemandTask(sodbuild);
                ScanOnDemandQueue.queue(task);
            }
        }
        rsp.sendRedirect("../");
    }
}
