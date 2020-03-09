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
package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.providers.FailureCauseProvider;
import hudson.model.Run;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the FailureCauseProvider,
 * used to prepare build information for the MQ Notification plugin.
 * @author Tomas Westling &lt;tomas.westling@axis.com&gt;
 */
public class FailureCauseProviderTest {
    private static final int MATCHING_LINE_NUMBER = 10;
    /**
     * Tests that the Json structure is correct after the FailureCauseProvider has done its thing.
     */
    @Test
    public void testCorrectJson() {
        List<String> categories = new ArrayList<String>();
        categories.add("category1");
        categories.add("category2");
        //Create the needed objects in order to test the FailureCauseProvider.
        //The null values in the constructor are the ones that don't end up in the resulting
        //Json structure, namely comment, date, modifications and indications (which is added directly
        //as a FoundIndication in the Action.
        FailureCause failureCause = new FailureCause(
                "myid",
                "myname",
                "mydescription",
                null,
                null,
                categories,
                null,
                null);
        List<FoundIndication> foundIndications = new ArrayList<FoundIndication>();
        FoundIndication foundIndication = new FoundIndication(null, "mypattern", "myfile", "mystring",
            MATCHING_LINE_NUMBER);
        foundIndications.add(foundIndication);
        FoundFailureCause foundFailureCause = new FoundFailureCause(failureCause, foundIndications);
        List<FoundFailureCause> foundFailureCauses = new ArrayList<FoundFailureCause>();
        foundFailureCauses.add(foundFailureCause);
        FailureCauseBuildAction action = new FailureCauseBuildAction(foundFailureCauses);
        Run run = mock(Run.class);
        when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        FailureCauseProvider provider = new FailureCauseProvider();

        //Fill the JSON object with the data from the FailureCauseProvider
        JSONObject json = new JSONObject();
        provider.provideCompletedRunData(run, json);

        //Check that the correct values are in place in the Json.
        JSONArray failureCausesjson = (JSONArray)json.get("failurecauses");
        JSONObject specificFailureCausesJson = (JSONObject)failureCausesjson.get(0);
        List<String> cat = (List<String>)specificFailureCausesJson.get("categories");
        JSONArray indicationsJson = (JSONArray)specificFailureCausesJson.get("indications");
        assertThat(specificFailureCausesJson.getString("id"), is("myid"));
        assertThat(specificFailureCausesJson.getString("name"), is("myname"));
        assertThat(specificFailureCausesJson.getString("description"), is("mydescription"));
        assertThat(cat.get(0), is("category1"));
        assertThat(cat.get(1), is("category2"));
        assertThat(((JSONObject)indicationsJson.get(0)).getString("pattern"), is("mypattern"));
        assertThat(((JSONObject)indicationsJson.get(0)).getString("matchingString"), is("mystring"));
        assertThat(((JSONObject)indicationsJson.get(0)).getInt("matchingLine"), is(MATCHING_LINE_NUMBER));
    }
}
