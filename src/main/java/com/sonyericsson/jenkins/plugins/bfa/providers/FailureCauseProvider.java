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
            List<FoundFailureCause> foundFailureCausesList = action.getFoundFailureCauses();
            JSONArray failureCausesJSONArray = new JSONArray();
            for (FoundFailureCause foundFailureCause : foundFailureCausesList) {
                JSONObject failureCauseJSONObject = new JSONObject();
                failureCauseJSONObject.put("id", foundFailureCause.getId());
                failureCauseJSONObject.put("name", foundFailureCause.getName());
                failureCauseJSONObject.put("description", foundFailureCause.getDescription());
                failureCauseJSONObject.put("categories", foundFailureCause.getCategories());
                JSONArray foundIndicationsJSONArray = new JSONArray();
                for (FoundIndication indication : foundFailureCause.getIndications()) {
                    JSONObject foundIndicationJSONObject = new JSONObject();
                    foundIndicationJSONObject.put("pattern", indication.getPattern());
                    foundIndicationJSONObject.put("matchingString", indication.getMatchingString());
                    foundIndicationJSONObject.put("matchingLine", indication.getMatchingLine());
                    foundIndicationsJSONArray.add(foundIndicationJSONObject);
                }
                failureCauseJSONObject.put("indications", foundIndicationsJSONArray);
                failureCausesJSONArray.add(failureCauseJSONObject);
            }
            json.put("failurecauses", failureCausesJSONArray);
        }
    }
}
