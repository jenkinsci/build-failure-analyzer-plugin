/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications AB. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.util.RunList;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.matrix.MatrixBuild;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.junit.Assert.assertEquals;


//CS IGNORE MagicNumber FOR NEXT 300 LINES. REASON: TestData.
/**
 * Tests for the ScanOnDemandTask.
 *
 * @author shemeer.x.sulaiman@sonymobile.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, PluginImpl.class, ScanOnDemandQueue.class, ScanOnDemandTask.class })
public class ScanOnDemandTaskTest {

    private ScanOnDemandBaseAction sodbaseaction;
    private AbstractProject mockproject;
    private PluginImpl pluginMock;

    /**
     * Runs before every test.
     * Mocks {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getInstance()} to avoid NPE's
     * when checking permissions in the code under test.
     */
    @Before
    public void setUp() {
        pluginMock = PowerMockito.mock(PluginImpl.class);
        mockStatic(PluginImpl.class);
        when(PluginImpl.getInstance()).thenReturn(pluginMock);
    }

    /**
     * Happy test that should find one non scanned build found
     * due to build failure.
     *
     * @throws Exception if so.
     */
    @Test
    public void testOneSODbuildfoundwithBuildFailure() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        AbstractBuild mockbuild1 = PowerMockito.mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = PowerMockito.mock(AbstractBuild.class);
        PowerMockito.when(mockbuild1.getResult()).thenReturn(Result.SUCCESS);
        PowerMockito.when(mockbuild2.getResult()).thenReturn(Result.FAILURE);
        sodbuilds.add(mockbuild1);
        sodbuilds.add(mockbuild2);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 1, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find two non scanned build found
     * due to build failure.
     *
     * @throws Exception if so.
     */
    @Test
    public void testTwoSODbuildfoundwithBuildFailure() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        AbstractBuild mockbuild1 = PowerMockito.mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = PowerMockito.mock(AbstractBuild.class);
        PowerMockito.when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        PowerMockito.when(mockbuild2.getResult()).thenReturn(Result.FAILURE);
        sodbuilds.add(mockbuild1);
        sodbuilds.add(mockbuild2);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 2, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find no SOD build because
     * all buld are success.
     *
     */
    @Test
    public void testNoSODbuildfoundwithBuildSuccess() {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        AbstractBuild mockbuild1 = PowerMockito.mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = PowerMockito.mock(AbstractBuild.class);
        PowerMockito.when(mockbuild1.getResult()).thenReturn(Result.SUCCESS);
        PowerMockito.when(mockbuild2.getResult()).thenReturn(Result.SUCCESS);
        sodbuilds.add(mockbuild1);
        sodbuilds.add(mockbuild2);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 0, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find no SOD build because
     * all buld are failed but already scanned.
     *
     * @throws Exception if so.
     */
    @Test
    public void testNoSODbuildfoundwithBuildFailedButAlreadyScanned() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();

        List<FoundFailureCause> foundFailureCauses = new ArrayList<FoundFailureCause>();
        foundFailureCauses.add(new FoundFailureCause(configureCauseAndIndication()));
        List<FailureCauseBuildAction> failureCauseBuildActions = new ArrayList<FailureCauseBuildAction>();
        failureCauseBuildActions.add(new FailureCauseBuildAction(foundFailureCauses));
        AbstractBuild mockbuild1 = PowerMockito.mock(AbstractBuild.class);
        PowerMockito.when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        FailureCauseBuildAction failureCauseBuildAction = PowerMockito.mock(FailureCauseBuildAction.class);
        mockbuild1.addAction(failureCauseBuildAction);
        sodbuilds.add(mockbuild1);
        PowerMockito.when(mockbuild1.getActions(FailureCauseBuildAction.class)).thenReturn(failureCauseBuildActions);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 0, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find one matrixbuild need
     * to be scanned.
     *
     * @throws Exception if so.
     */
    @Test
    public void testOneSODMatrixbuildfoundwithBuildFailure() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        MatrixBuild matrixbuild1 = PowerMockito.mock(MatrixBuild.class);
        MatrixBuild matrixbuild2 = PowerMockito.mock(MatrixBuild.class);
        PowerMockito.when(matrixbuild1.getResult()).thenReturn(Result.SUCCESS);
        PowerMockito.when(matrixbuild2.getResult()).thenReturn(Result.FAILURE);
        sodbuilds.add(matrixbuild1);
        sodbuilds.add(matrixbuild2);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 1, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find no matrixbuild since
     * all builds are success.
     *
     * @throws Exception if so.
     */
    @Test
    public void testNoSODMatrixBuildfoundwithBuildSuccess() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        MatrixBuild matrixbuild1 = PowerMockito.mock(MatrixBuild.class);
        MatrixBuild matrixbuild2 = PowerMockito.mock(MatrixBuild.class);
        PowerMockito.when(matrixbuild1.getResult()).thenReturn(Result.SUCCESS);
        PowerMockito.when(matrixbuild2.getResult()).thenReturn(Result.SUCCESS);
        sodbuilds.add(matrixbuild1);
        sodbuilds.add(matrixbuild2);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 0, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Happy test that should find no matrixbuild since
     * build failed but already scanned.
     *
     * @throws Exception if so.
     */
    @Test
    public void testNoSODmatrixbuildfoundwithBuildFailedButAlreadyScanned() throws Exception {
        mockproject = PowerMockito.mock(AbstractProject.class);
        RunList sodbuilds = new RunList();
        List<FoundFailureCause> foundFailureCauses = new ArrayList<FoundFailureCause>();
        foundFailureCauses.add(new FoundFailureCause(configureCauseAndIndication()));
        List<FailureCauseBuildAction> failureCauseBuildActions = new ArrayList<FailureCauseBuildAction>();
        failureCauseBuildActions.add(new FailureCauseBuildAction(foundFailureCauses));
        MatrixBuild mockbuild1 = PowerMockito.mock(MatrixBuild.class);
        PowerMockito.when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        FailureCauseBuildAction failureCauseBuildAction = PowerMockito.mock(FailureCauseBuildAction.class);
        mockbuild1.addAction(failureCauseBuildAction);
        sodbuilds.add(mockbuild1);
        PowerMockito.when(mockbuild1.getActions(FailureCauseBuildAction.class)).thenReturn(failureCauseBuildActions);
        PowerMockito.when(mockproject.getBuilds()).thenReturn(sodbuilds);
        sodbaseaction = new ScanOnDemandBaseAction(mockproject);
        assertEquals("Nonscanned buils", 0, sodbaseaction.getNotScannedBuilds().size());
    }

    /**
     * Convenience method for a standard cause that finds {@link #TO_PRINT} in the build log.
     *
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     * @see #configureCauseAndIndication(String, String,
     * com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication() throws Exception {
        return configureCauseAndIndication(new BuildLogIndication(".*ERROR.*"));
    }

    //CS IGNORE LineLength FOR NEXT 10 LINES. REASON: JavaDoc.
    /**
     * Convenience method for the standard cause with a special indication.
     *
     * @param indication the indication for the cause.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     *
     * @see #configureCauseAndIndication(String, String, com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private FailureCause configureCauseAndIndication(Indication indication) throws Exception {
        return configureCauseAndIndication("Error", "This is an error", indication);
    }

    /**
     * Convenience method for a standard cause with a category and the provided indication.
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     */
    private FailureCause configureCauseAndIndication(String name, String description, Indication indication)
            throws Exception {
        return configureCauseAndIndication(name, description, "category", indication);
    }

    /**
     * Configures the global settings with a cause that has the provided indication/
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param category    the category of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     * @throws Exception if something goes wrong in handling the causes.
     */
    private FailureCause configureCauseAndIndication(String name, String description, String category,
            Indication indication) throws Exception {
        List<Indication> indicationList = new LinkedList<Indication>();
        indicationList.add(indication);
        FailureCause failureCause = new FailureCause(name, name, description, category, indicationList);

        List<FailureCause> causeList = new LinkedList<FailureCause>();
        causeList.add(failureCause);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(causeList));
        return failureCause;
    }
}

