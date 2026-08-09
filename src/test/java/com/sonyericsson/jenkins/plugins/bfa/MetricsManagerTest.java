package com.sonyericsson.jenkins.plugins.bfa;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.IFailureCauseMetricData;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.model.Run;
import jenkins.metrics.api.Metrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.addMetric;
import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.incCounters;
import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.addJobBuildCausesMetric;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link MetricsManager}.
 */
class MetricsManagerTest {
    private MetricRegistry metricRegistry;
    private Counter counter;

    private String mockedJobDisplayName;
    private Run mockedJobBuild;
    private FailureCause mockedCause;
    private List<? extends IFailureCauseMetricData> mockedCauseList;
    private MockedStatic<Metrics> metricsMockedStatic;

    /**
     * Common stuff to set up for the tests.
     */
    @BeforeEach
    void setUp() {
        metricRegistry = mock(MetricRegistry.class);
        counter = mock(Counter.class);
        List<Indication> indications = new LinkedList<>();
        mockedJobDisplayName = "myJob";
        mockedJobBuild = mock(Run.class);
        Indication indication = new BuildLogIndication("something");
        indications.add(indication);
        mockedCause = new FailureCause("id", "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);
        mockedCauseList = new ArrayList<>(Arrays.asList(mockedCause, mockedCause));

        metricsMockedStatic = mockStatic(Metrics.class);
        metricsMockedStatic.when(Metrics::metricRegistry).thenReturn(metricRegistry);

        when(metricRegistry.counter(anyString())).thenReturn(counter);
        when(mockedJobBuild.getNumber()).thenReturn(1);
    }

    /**
     * Release all the static mocks.
     */
    @AfterEach
    void tearDown() {
        metricsMockedStatic.close();
    }

    /**
     * Test that the case and category counters are created from a FailureCause.
     */
    @Test
    void testAddMetric() {
        addMetric(mockedCause);

        verify(metricRegistry, times(1)).counter("jenkins_bfa.cause.myFailureCause");
        verify(metricRegistry, times(1)).counter("jenkins_bfa.category.category");
    }

    /**
     * Test that the cause and category counters are incremented twice for a FailureCause when not using squashing.
     */
    @Test
    void testIncCountersWithSquashingDisabled() {
        incCounters(mockedCauseList, false);

        verify(counter, times(mockedCauseList.size() * 2)).inc();
    }

    /**
     * Test that the cause and category counters are incremented once for a FailureCause when using squashing.
     */
    @Test
    void testIncCountersWithSquashingEnabled() {
        incCounters(mockedCauseList, true);

        verify(counter, times(mockedCauseList.size())).inc();
    }

    /**
     * Test that a job counter is incremented for found failure causes in a build.
     */
    @Test
    public void testaddJobBuildCausesMetric() {
        addJobBuildCausesMetric(mockedJobDisplayName, mockedJobBuild, mockedCauseList);

        verify(metricRegistry, times(2)).counter("jenkins_bfa.job.@myJob@.number.@1@.cause.@myFailureCause");
    }
}
