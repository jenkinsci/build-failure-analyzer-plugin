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

package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
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
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Looks for Indications, trying to find the Cause of a problem.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureScanner extends Notifier implements MatrixAggregatable {
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
        if (PluginImpl.shouldScan(build) && build.getResult().isWorseThan(Result.SUCCESS)) {
            PrintStream buildLog = buildListener.getLogger();
            Collection<FailureCause> causes = PluginImpl.getInstance().getKnowledgeBase().getCauses();
            List<FoundFailureCause> foundCauseList = findCauses(causes, build, buildLog);
            FailureCauseBuildAction buildAction = new FailureCauseBuildAction(foundCauseList);
            build.addAction(buildAction);
        }
        return true;
    }

    /**
     * Finds the failure causes for this build.
     *
     * @param causes the list of possible causes.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return a list of found failure causes.
     */
    private List<FoundFailureCause> findCauses(Collection<FailureCause> causes,
                                               AbstractBuild build, PrintStream buildLog) {
        List<FoundFailureCause> foundFailureCauseList = new LinkedList<FoundFailureCause>();
        for (FailureCause cause : causes) {
            List<FoundIndication> foundIndications = findIndications(cause, build, buildLog);
            if (!foundIndications.isEmpty()) {
                FoundFailureCause foundFailureCause = new FoundFailureCause(cause);
                foundFailureCause.addIndications(foundIndications);
                foundFailureCauseList.add(foundFailureCause);
            }
        }
        return foundFailureCauseList;
    }

    /**
     * Finds the indications of a failure cause.
     *
     * @param cause the cause to find indications for.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return a list of found indications for a cause.
     */
    private List<FoundIndication> findIndications(FailureCause cause, AbstractBuild build, PrintStream buildLog) {
        List<Indication> indicationList = cause.getIndications();
        List<FoundIndication> foundIndicationList = new LinkedList<FoundIndication>();
        for (Indication indication : indicationList) {
            FoundIndication foundIndication = findIndication(indication, build, buildLog);
            if (foundIndication != null) {
                foundIndicationList.add(foundIndication);
            }
        }
        return foundIndicationList;
    }

    /**
     * Finds out if this indication matches the build.
     *
     * @param indication the indication to look for.
     * @param build the build to analyze.
     * @param buildLog the build log.
     * @return an indication if one is found, null otherwise.
     */
    private FoundIndication findIndication(Indication indication, AbstractBuild build, PrintStream buildLog) {
        FailureReader failureReader = indication.getReader();
        return failureReader.scan(build, buildLog);
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
