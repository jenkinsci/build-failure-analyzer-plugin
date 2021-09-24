package com.sonyericsson.jenkins.plugins.bfa;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import jenkins.metrics.api.Metrics;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.addMetric;
import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.incCounters;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Tests for {@link MetricsManager}.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({MetricRegistry.class})
public class MetricsManagerTest {
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private Counter counter;

    private List<Indication> indications;
    private Indication indication;
    private FailureCause mockedCause;


    /**
     * Common stuff to set up for the tests.
     */
    @Before
    public void setUp() {
        indications = new LinkedList<Indication>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
        mockedCause = new FailureCause("id", "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);

        PowerMockito.mockStatic(Metrics.class);
        PowerMockito.when(Metrics.metricRegistry()).thenReturn(metricRegistry);
        PowerMockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);
    }

    /**
     * Test that the case and category counters are created from a FailureCause.
     */
    public void testAddMetric() {
        addMetric(mockedCause);

        verify(metricRegistry, times(1)).counter("jenkins_bfa.cause.myFailureCause");
        verify(metricRegistry, times(1)).counter("jenkins_bfa.category.category");
    }

    /**
     * Test that the cause and category counters are incremented for a Failurecasue.
     */
    public void testIncCounters() {
        incCounters(mockedCause);

        verify(metricRegistry, times(1)).counter("jenkins_bfa.cause.myFailureCause").inc();
        verify(metricRegistry, times(1)).counter("jenkins_bfa.category.category").inc();
    }

}
