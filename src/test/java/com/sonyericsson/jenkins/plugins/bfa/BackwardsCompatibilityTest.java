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

import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.List;

import static hudson.Util.fixEmpty;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData

/**
 * Tests that the plugin can upgrade existing old data.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class BackwardsCompatibilityTest extends HudsonTestCase {

    /**
     * Tests that a build containing version 1 of {@link FailureCauseBuildAction} can be done.
     */
    @LocalData
    public void testReadResolveFromVersion1() {
        FreeStyleProject job = (FreeStyleProject)Jenkins.getInstance().getItem("bfa");
        assertNotNull(job);
        FailureCauseBuildAction action = job.getBuilds().getFirstBuild().getAction(FailureCauseBuildAction.class);
        List<FoundFailureCause> foundFailureCauses = Whitebox.getInternalState(action, "foundFailureCauses");
        List<FailureCause> failureCauses = Whitebox.getInternalState(action, "failureCauses");
        assertNotNull(foundFailureCauses);
        assertTrue(foundFailureCauses.isEmpty());
        assertNull(failureCauses);


        action = job.getBuilds().getLastBuild().getAction(FailureCauseBuildAction.class);
        foundFailureCauses = Whitebox.getInternalState(action, "foundFailureCauses");
        failureCauses = Whitebox.getInternalState(action, "failureCauses");
        assertNotNull(foundFailureCauses);
        assertEquals(1, foundFailureCauses.size());
        assertNull(failureCauses);
    }

    /**
     * Tests that legacy causes in {@link PluginImpl#causes} gets converted during startup to a {@link
     * com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase}.
     * @throws Exception if so.
     */
    @LocalData
    public void testLoadVersion1ConfigXml() throws Exception {
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        Collection<FailureCause> causes = knowledgeBase.getCauses();
        assertEquals(3, causes.size());
        Indication indication = null;
        for (FailureCause c : causes) {
            assertNotNull(c.getName() + " should have an id", fixEmpty(c.getId()));
            if ("The Wrong".equals(c.getName())) {
                indication = c.getIndications().get(0);
            }
        }
        assertNotNull("Missing a cause!", indication);
        assertEquals(".+wrong.*", Whitebox.getInternalState(indication, "pattern").toString());
    }
}
