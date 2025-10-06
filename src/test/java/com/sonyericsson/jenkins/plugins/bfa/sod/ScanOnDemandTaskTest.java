/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications Inc. All rights reserved.
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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for the ScanOnDemandTask.
 *
 * @author shemeer.x.sulaiman@sonymobile.com&gt;
 */
class ScanOnDemandTaskTest {

    private AbstractProject mockproject;
    private PluginImpl pluginMock;

    private Jenkins jenkins;
    private Metrics metricsPlugin;
    private MetricRegistry metricRegistry;
    private MockedStatic<PluginImpl> pluginMockedStatic;
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<Metrics> metricsMockedStatic;

    /**
     * Runs before every test.
     * Mocks {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getInstance()} to avoid NPE's
     * when checking permissions in the code under test.
     */
    @BeforeEach
    void setUp() {
        jenkins = mock(Jenkins.class);
        metricsPlugin = mock(Metrics.class);
        pluginMock = mock(PluginImpl.class);
        metricRegistry = mock(MetricRegistry.class);
        pluginMockedStatic = mockStatic(PluginImpl.class);
        pluginMockedStatic.when(PluginImpl::getInstance).thenReturn(pluginMock);
        pluginMockedStatic.when(() -> PluginImpl.needToAnalyze(Result.FAILURE)).thenReturn(true);

        jenkinsMockedStatic = mockStatic(Jenkins.class);
        metricsMockedStatic = mockStatic(Metrics.class);
        jenkinsMockedStatic.when(Jenkins::get).thenReturn(jenkins);
        when(jenkins.getPlugin(Metrics.class)).thenReturn(metricsPlugin);
        metricsMockedStatic.when(Metrics::metricRegistry).thenReturn(metricRegistry);
    }

    /**
     * Release all the static mocks.
     */
    @AfterEach
    void tearDown() {
        jenkinsMockedStatic.close();
        pluginMockedStatic.close();
        metricsMockedStatic.close();
    }

    /**
     * Happy test that should find one non scanned build found
     * due to build failure.
     *
     */
    @Test
    void testOneSODbuildfoundwithBuildFailure() {
        mockproject = mock(AbstractProject.class);
        AbstractBuild mockbuild1 = mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = mock(AbstractBuild.class);
        when(mockbuild1.getResult()).thenReturn(Result.SUCCESS);
        when(mockbuild2.getResult()).thenReturn(Result.FAILURE);
        RunList<AbstractBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Arrays.asList(mockbuild1, mockbuild2));
        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(1, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find two non scanned build found
     * due to build failure.
     *
     */
    @Test
    void testTwoSODbuildfoundwithBuildFailure() {
        mockproject = mock(AbstractProject.class);

        AbstractBuild mockbuild1 = mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = mock(AbstractBuild.class);
        when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        when(mockbuild2.getResult()).thenReturn(Result.FAILURE);
        RunList<AbstractBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Arrays.asList(mockbuild1, mockbuild2));

        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(2, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find no SOD build because
     * all buld are success.
     *
     */
    @Test
    void testNoSODbuildfoundwithBuildSuccess() {
        mockproject = mock(AbstractProject.class);
        AbstractBuild mockbuild1 = mock(AbstractBuild.class);
        AbstractBuild mockbuild2 = mock(AbstractBuild.class);
        when(mockbuild1.getResult()).thenReturn(Result.SUCCESS);
        when(mockbuild2.getResult()).thenReturn(Result.SUCCESS);

        RunList<AbstractBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Arrays.asList(mockbuild1, mockbuild2));
        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(0, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find no SOD build because
     * all buld are failed but already scanned.
     *
     */
    @Test
    void testNoSODbuildfoundwithBuildFailedButAlreadyScanned() {
        mockproject = mock(AbstractProject.class);

        List<FoundFailureCause> foundFailureCauses = new ArrayList<>();
        foundFailureCauses.add(new FoundFailureCause(configureCauseAndIndication()));
        List<FailureCauseBuildAction> failureCauseBuildActions = new ArrayList<>();
        failureCauseBuildActions.add(new FailureCauseBuildAction(foundFailureCauses));
        AbstractBuild mockbuild1 = mock(AbstractBuild.class);
        when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        FailureCauseBuildAction failureCauseBuildAction = mock(FailureCauseBuildAction.class);
        mockbuild1.addAction(failureCauseBuildAction);

        RunList<AbstractBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Collections.singletonList(mockbuild1));

        when(mockbuild1.getActions(FailureCauseBuildAction.class)).thenReturn(failureCauseBuildActions);
        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(0, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find one matrixbuild need
     * to be scanned.
     *
     */
    @Test
    void testOneSODMatrixbuildfoundwithBuildFailure() {
        mockproject = mock(AbstractProject.class);

        MatrixBuild matrixbuild1 = mock(MatrixBuild.class);
        MatrixBuild matrixbuild2 = mock(MatrixBuild.class);
        when(matrixbuild1.getResult()).thenReturn(Result.SUCCESS);
        when(matrixbuild2.getResult()).thenReturn(Result.FAILURE);

        RunList<MatrixBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Arrays.asList(matrixbuild1, matrixbuild2));

        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(1, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find no matrixbuild since
     * all builds are success.
     *
     */
    @Test
    void testNoSODMatrixBuildfoundwithBuildSuccess() {
        mockproject = mock(AbstractProject.class);

        MatrixBuild matrixbuild1 = mock(MatrixBuild.class);
        MatrixBuild matrixbuild2 = mock(MatrixBuild.class);
        when(matrixbuild1.getResult()).thenReturn(Result.SUCCESS);
        when(matrixbuild2.getResult()).thenReturn(Result.SUCCESS);

        RunList<MatrixBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Arrays.asList(matrixbuild1, matrixbuild2));

        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(0, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Happy test that should find no matrixbuild since
     * build failed but already scanned.
     *
     */
    @Test
    void testNoSODmatrixbuildfoundwithBuildFailedButAlreadyScanned() {
        mockproject = mock(AbstractProject.class);

        List<FoundFailureCause> foundFailureCauses = new ArrayList<>();
        foundFailureCauses.add(new FoundFailureCause(configureCauseAndIndication()));
        List<FailureCauseBuildAction> failureCauseBuildActions = new ArrayList<>();
        failureCauseBuildActions.add(new FailureCauseBuildAction(foundFailureCauses));
        MatrixBuild mockbuild1 = mock(MatrixBuild.class);
        when(mockbuild1.getResult()).thenReturn(Result.FAILURE);
        FailureCauseBuildAction failureCauseBuildAction = mock(FailureCauseBuildAction.class);
        mockbuild1.addAction(failureCauseBuildAction);

        RunList<AbstractBuild> builds = new RunList<>(Collections.emptyList());
        Whitebox.setInternalState(builds, "base", Collections.singletonList(mockbuild1));

        when(mockbuild1.getActions(FailureCauseBuildAction.class)).thenReturn(failureCauseBuildActions);
        when(mockproject.getBuilds()).thenReturn(builds);
        ScanOnDemandBaseAction.NonScanned action = new ScanOnDemandBaseAction.NonScanned();
        assertEquals(0, Lists.newArrayList(action.getRuns(mockproject)).size(), "Nonscanned buils");
    }

    /**
     * Convenience method for a standard cause that finds ERROR in the build log.
     *
     * @return the configured cause that was added to the global config.
     * @see #configureCauseAndIndication(com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     * @see #configureCauseAndIndication(String, String,
     * com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private static FailureCause configureCauseAndIndication() {
        return configureCauseAndIndication(new BuildLogIndication(".*ERROR.*"));
    }

    /**
     * Convenience method for the standard cause with a special indication.
     *
     * @param indication the indication for the cause.
     * @return the configured cause that was added to the global config.
     * @see #configureCauseAndIndication(String, String,
     *        com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication)
     */
    private static FailureCause configureCauseAndIndication(Indication indication) {
        return configureCauseAndIndication("Error", "This is an error", indication);
    }

    /**
     * Convenience method for a standard cause with a category and the provided indication.
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     */
    private static FailureCause configureCauseAndIndication(String name, String description, Indication indication) {
        return configureCauseAndIndication(name, description, "comment", "category", indication);
    }

    /**
     * Configures the global settings with a cause that has the provided indication/
     *
     * @param name        the name of the cause.
     * @param description the description of the cause.
     * @param comment     the comment of the cause.
     * @param category    the category of the cause.
     * @param indication  the indication.
     * @return the configured cause that was added to the global config.
     */
    private static FailureCause configureCauseAndIndication(String name, String description, String comment,
            String category, Indication indication) {
        List<Indication> indicationList = new LinkedList<>();
        indicationList.add(indication);
        FailureCause failureCause =
                new FailureCause(name, name, description, comment, null, category, indicationList, null);

        List<FailureCause> causeList = new LinkedList<>();
        causeList.add(failureCause);
        Whitebox.setInternalState(PluginImpl.getInstance(), KnowledgeBase.class, new LocalFileKnowledgeBase(causeList));
        return failureCause;
    }
}

