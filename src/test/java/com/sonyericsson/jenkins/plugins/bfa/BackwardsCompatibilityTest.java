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
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import com.sonyericsson.jenkins.plugins.bfa.utils.OldDataConverter;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static hudson.Util.fixEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//CS IGNORE MagicNumber FOR NEXT 160 LINES. REASON: TestData

/**
 * Tests that the plugin can upgrade existing old data.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@WithJenkins
class BackwardsCompatibilityTest {

    /**
     * Tests that a build containing version 1 of {@link FailureCauseBuildAction} can be done.
     *
     * @param jenkins
     */
    @LocalData
    @Test
    void testReadResolveFromVersion1(JenkinsRule jenkins) {
        FreeStyleProject job = (FreeStyleProject)Jenkins.get().getItem("bfa");
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
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @LocalData
    @Test
    void testLoadVersion1ConfigXml(JenkinsRule jenkins) throws Exception {
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        Collection<FailureCause> causes = knowledgeBase.getCauses();
        assertEquals(3, causes.size());
        Indication indication = null;
        for (FailureCause c : causes) {
            assertNotNull(fixEmpty(c.getId()), c.getName() + " should have an id");
            if ("The Wrong".equals(c.getName())) {
                indication = c.getIndications().get(0);
            }
        }
        assertNotNull(indication, "Missing a cause!");
        assertEquals(".+wrong.*", Whitebox.getInternalState(indication, "pattern").toString());
    }

    /**
     * Tests that a legacy FoundFailureCause can be loaded by the annotator.
     *
     * @param jenkins
     *
     * @throws Exception if so.
     */
    @LocalData
    @Test
    void testLoadOldFailureCauseWithOnlyLineNumbers(JenkinsRule jenkins) throws Exception {
        FreeStyleProject job = (FreeStyleProject)Jenkins.get().getItem("MyProject");
        assertNotNull(job);
        FreeStyleBuild build = job.getBuilds().getFirstBuild();
        OldDataConverter.getInstance().waitForInitialCompletion();
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        List<FoundFailureCause> foundFailureCauses = Whitebox.getInternalState(action, "foundFailureCauses");
        FoundFailureCause foundFailureCause = foundFailureCauses.get(0);
        FoundIndication indication = foundFailureCause.getIndications().get(0);
        assertTrue(indication.getMatchingString().matches(indication.getPattern()));
        IndicationAnnotator annotator = new IndicationAnnotator(foundFailureCauses);
        Map<String, AnnotationHelper> helperMap = Whitebox.getInternalState(annotator, "helperMap");
        //since the old FoundIndication doesn't contain a matchingString from the start, we check it.
        AnnotationHelper annotationHelper = helperMap.get(indication.getMatchingString());
        assertNotNull(annotationHelper);
    }

    /**
     * Tests if a {@link MatrixBuild} gets loaded and converted correctly from a version 1.2.0 save.
     *
     * @param jenkins
     *
     * @throws InterruptedException if it is not allowed to sleep in the beginning.
     */
    @LocalData
    @Test
    void testMatrix120(JenkinsRule jenkins) throws InterruptedException {
        MatrixProject project = (MatrixProject)jenkins.jenkins.getItem("mymatrix");
        MatrixBuild build = project.getBuildByNumber(1);
        MatrixBuild build2 = project.getBuildByNumber(2);
        OldDataConverter.getInstance().waitForInitialCompletion();
        FailureCauseMatrixBuildAction matrixBuildAction = build.getAction(FailureCauseMatrixBuildAction.class);
        assertNotNull(matrixBuildAction);
        List<MatrixRun> runs = Whitebox.getInternalState(matrixBuildAction, "runs");
        assertNotNull(runs);
        List<String> runIds = null;

        runIds = Whitebox.getInternalState(matrixBuildAction, "runIds");

        assertEquals(runs.size(), runIds.size());
        assertNotNull(runs.get(3).getProject());
        assertEquals(runs.get(3).getProject().getCombination().toString(), runIds.get(3));
        assertNotNull(Whitebox.getInternalState(matrixBuildAction, "build"));

        List<MatrixRun> aggregatedRuns2 = FailureCauseMatrixAggregator.getRuns(build2);
        FailureCauseMatrixBuildAction matrixBuildAction2 = build2.getAction(FailureCauseMatrixBuildAction.class);
        assertNotNull(matrixBuildAction2);
        List<MatrixRun> runs2 = Whitebox.getInternalState(matrixBuildAction2, "runs");
        assertSame(aggregatedRuns2.get(5), runs2.get(5));
    }
}
