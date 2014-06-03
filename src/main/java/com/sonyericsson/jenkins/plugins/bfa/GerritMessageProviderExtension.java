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
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

/**
 * ExtensionPoint that allows BFA to send the failure cause description
 * directly to Gerrit.
 *
 * @author Gustaf Lundh &lt;gustaf.lundh@sonymobile.com&gt;
 */
@Extension(optional = true)
public class GerritMessageProviderExtension extends GerritMessageProvider {

    @Override
    public String getBuildCompletedMessage(AbstractBuild build) {
        if (PluginImpl.getInstance().isGerritTriggerEnabled()) {
            StringBuilder customMessage = new StringBuilder();
            if (build != null) {
                FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
                if (action != null) {
                    FailureCauseDisplayData displayData = action.getFailureCauseDisplayData();
                    
                    addFailureCausesFromData(customMessage, displayData);
                    for (FailureCauseDisplayData downstreamCause : displayData.getDownstreamFailureCauses()) {
                        addFailureCausesFromData(customMessage, downstreamCause);
                    }
                    
                    if (customMessage.length() > 0) {
                        return customMessage.toString().replaceAll("'", "\\'");
                    }
                }
            }
        }
        return null;
    }

    private void addFailureCausesFromData(StringBuilder customMessage, FailureCauseDisplayData downstreamCause) {
        for (FoundFailureCause failureCause : downstreamCause.getFoundFailureCauses()) {
            if (customMessage.length() > 0) {
                customMessage.append("\n\n");
            }
            customMessage.append(failureCause.getDescription());

            FoundIndication indication = failureCause.getIndications().get(0);
            customMessage.append(" ( ")
            .append("  ")
            .append(Hudson.getInstance().getRootUrl())
            .append('/')
            .append(indication.getBuild().getUrl())
            .append(" )");
        }
    }
}
