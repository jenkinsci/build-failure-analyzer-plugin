/*
 *  The MIT License
 *
 *  Copyright 2017 Axis Communications AB. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.bfa.providers;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.Extension;
import hudson.model.Run;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.sonymobile.jenkins.plugins.mq.mqnotifier.providers.MQDataProvider;

import java.util.List;

/**
 * Provides information about the failure causes for a build.
 *
 * @author Tomas Westling &lt;tomas.westling@axis.com&gt;
 */
@Extension(optional = true)
public class FailureCauseProvider extends MQDataProvider {

    @Override
    public void provideCompletedRunData(Run run, JSONObject json) {
        FailureCauseBuildAction action = run.getAction(FailureCauseBuildAction.class);
        if (action != null) {
            List<FoundFailureCause> foundFailureCauses = action.getFoundFailureCauses();
            JSONArray failureCausesJSON = new JSONArray();
            for (FoundFailureCause foundFailureCause : foundFailureCauses) {
                JSONObject failureCauseJSON = new JSONObject();
                failureCauseJSON.put("id", foundFailureCause.getId());
                failureCauseJSON.put("name", foundFailureCause.getName());
                failureCauseJSON.put("description", foundFailureCause.getDescription());
                failureCauseJSON.put("categories", foundFailureCause.getCategories());
                JSONArray foundIndicationsJSON = new JSONArray();
                for (FoundIndication ind : foundFailureCause.getIndications()) {
                    JSONObject foundIndicationJSON = new JSONObject();
                    foundIndicationJSON.put("pattern", ind.getPattern());
                    foundIndicationJSON.put("matchingString", ind.getMatchingString());
                    foundIndicationsJSON.add(foundIndicationJSON);
                }
                failureCauseJSON.put("indications", foundIndicationsJSON);
                failureCausesJSON.add(failureCauseJSON);
            }
            json.put("failurecauses", failureCausesJSON);
        }
    }
}
