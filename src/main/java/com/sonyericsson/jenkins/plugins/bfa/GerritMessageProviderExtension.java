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
package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.hudson.plugins.gerrit.trigger.gerritnotifier.GerritMessageProvider;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;

import hudson.Extension;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * ExtensionPoint that allows BFA to send the failure cause description
 * directly to Gerrit.
 *
 * @author Gustaf Lundh &lt;gustaf.lundh@sonymobile.com&gt;
 */
@Extension(optional = true)
public class GerritMessageProviderExtension extends GerritMessageProvider {

    @Override
    public String getBuildCompletedMessage(Run build) {
        if (PluginImpl.getInstance().isGerritTriggerEnabled()) {
            StringBuilder customMessage = new StringBuilder();
            if (build != null) {
                FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
                if (action != null) {
                    FailureCauseDisplayData displayData = action.getFailureCauseDisplayData();

                    addFailureCausesFromData(customMessage, displayData);
                    printDownstream(customMessage, displayData.getDownstreamFailureCauses());

                    if (customMessage.length() > 0) {
                        return customMessage.toString().replace("'", "&#39");
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * Adds all causes from downstream builds in recursion
     *
     * @param message the StringBuilder to add to.
     * @param downstreamFailureCauses the list of downstream failures.
     */
    private void printDownstream(StringBuilder message, List<FailureCauseDisplayData> downstreamFailureCauses) {
        if (!downstreamFailureCauses.isEmpty()) {
            for (FailureCauseDisplayData displayData : downstreamFailureCauses) {
                addFailureCausesFromData(message, displayData);
                printDownstream(message, displayData.getDownstreamFailureCauses());
            }
        }
    }

    /**
     * Appends FailureCause information to provided StringBuilder.
     *
     * @param message the StringBuilder to add to
     * @param displayData the data of downstream failures
     */
    private void addFailureCausesFromData(StringBuilder message, FailureCauseDisplayData displayData) {
        for (FoundFailureCause failureCause : displayData.getFoundFailureCauses()) {
            if (message.length() > 0) {
                message.append("\n\n");
            }
            message.append(failureCause.getDescription());

            message.append(" ( ")
            .append(Jenkins.getInstance().getRootUrl())
            .append(displayData.getLinks().getBuildUrl())
            .append(" )");
        }
    }
}
