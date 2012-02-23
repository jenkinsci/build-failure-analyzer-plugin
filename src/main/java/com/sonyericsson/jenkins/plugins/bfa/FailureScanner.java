/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Looks for Indications, trying to find the Cause of a problem.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureScanner extends Notifier implements MatrixAggregatable {

    private static final Logger logger = Logger.getLogger(FailureScanner.class.getName());
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, final Launcher launcher, final BuildListener buildListener) {
        if (PluginImpl.getInstance().isGlobalEnabled() && build.getResult().isWorseThan(Result.SUCCESS)) {
            PrintStream buildLog = buildListener.getLogger();
            List<FailureCause> causeList = PluginImpl.getInstance().getCauses().getView();
            causeList = findCauses(causeList, build, buildLog);
            FailureCauseBuildAction buildAction = new FailureCauseBuildAction(causeList);
            build.addAction(buildAction);
        }
        return true;
    }

    /**
     * Finds the failure causes for this build.
     *
     * @param causeList the list of possible causes.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return a list of failure causes.
     */
    private List<FailureCause> findCauses(List<FailureCause> causeList, AbstractBuild build, PrintStream buildLog) {
        List<FailureCause> returnList = new LinkedList<FailureCause>();
        for (FailureCause cause : causeList) {
            if (findIndications(cause, build, buildLog)) {
                returnList.add(cause);
            }
        }
        return returnList;
    }

    /**
     * Finds the indications of a failure cause.
     *
     * @param cause the cause to find indications for.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return true if this cause is the cause of the failure, false if not.
     */
    private boolean findIndications(FailureCause cause, AbstractBuild build, PrintStream buildLog) {
        List<Indication> indicationList = cause.getIndications();
        for (Indication indication : indicationList) {
            if (findIndication(indication, build, buildLog)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds out if this indication matches the build.
     *
     * @param indication the indication to look for.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return true if this indication matches, false if not.
     */
    private boolean findIndication(Indication indication, AbstractBuild build, PrintStream buildLog) {
        BufferedReader reader = null;
        boolean found = false;
        try {
            Pattern pattern = indication.getPattern();
            reader = new BufferedReader(indication.getReader(build));
            String line;
            while ((line = reader.readLine()) != null) {
                if (pattern.matcher(line).matches()) {
                    found = true;
                    break;
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "[BFA] I/O problems during indication analysis: ", ioe);
            buildLog.println("[BFA] I/O problems during indication analysis.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[BFA] Could not open reader for indication: ", e);
            buildLog.println("[BFA] Could not open reader for indication.");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close the reader. ", e);
                }
            }
        }
        return found;
    }

    @Override
    public FailureScannerDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(FailureScannerDescriptor.class);
    }

    @Override
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new FailureCauseMatrixAggregator(build, launcher, listener);
    }

    /**
     * Descriptor, apparently needed for the FailureScannerTest in order to add this notifier to the build.
     */
    public static final class FailureScannerDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
