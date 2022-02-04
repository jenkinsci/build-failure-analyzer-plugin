package com.sonyericsson.jenkins.plugins.bfa;

import com.codahale.metrics.MetricRegistry;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.IFailureCauseMetricData;
import jenkins.metrics.api.Metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public final class MetricsManager {
    static final String CAUSEPREFIX = "jenkins_bfa.cause.";
    static final String CATEGORYPREFIX = "jenkins_bfa.category.";

    /**A magic cause to represent builds that match no causes in the database. */
    public static final FailureCause UNKNOWNCAUSE = new FailureCause("no matching cause", "");

    private MetricsManager() {
    }

    private static Set<String> getMetricNames(IFailureCauseMetricData cause) {
        Set<String> metrics = new HashSet<String>();
        metrics.add(CAUSEPREFIX + cause.getName());
        List<String> categoriesForCause = cause.getCategories();
        if (categoriesForCause != null) {
            for (String string : categoriesForCause) {
                metrics.add(CATEGORYPREFIX + string);
            }
        }
        return metrics;
    }

    /**
     * Add metrics into the MetricRegistry from the Metrics plugin.
     *
     * @param cause The Cause to add metrics for
     */
    public static void addMetric(IFailureCauseMetricData cause) {
        MetricRegistry metricRegistry = Metrics.metricRegistry();
        SortedSet<String> existingMetrics = metricRegistry.getNames();
        Set<String> metrics = getMetricNames(cause);
        for (String metric : metrics) {
            if (!existingMetrics.contains(metric)) {
                metricRegistry.counter(metric);
            }
        }
    }

    /**
     * Increment counters for the metric and its categories.
     * @param causes The cause to increment counters for
     * @param squashCauses Whether or not to squash cause metrics
     */
    public static void incCounters(List<? extends IFailureCauseMetricData> causes, boolean squashCauses) {
        MetricRegistry metricRegistry = Metrics.metricRegistry();
        if (squashCauses) {
            Set<String> metrics = new HashSet<>();
            for (IFailureCauseMetricData cause : causes) {
                metrics.addAll(getMetricNames(cause));
            }
            for (String metric : metrics) {
                metricRegistry.counter(metric).inc();
            }
        } else {
            for (IFailureCauseMetricData cause : causes) {
                Set<String> metrics = getMetricNames(cause);
                for (String metric : metrics) {
                    metricRegistry.counter(metric).inc();
                }
            }
        }
    }
}
