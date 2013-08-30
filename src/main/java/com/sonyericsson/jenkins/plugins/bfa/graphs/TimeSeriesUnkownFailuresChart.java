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
package com.sonyericsson.jenkins.plugins.bfa.graphs;

import hudson.model.AbstractProject;

import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;

/**
 * Bar chart displaying the number of different failure causes for a project.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class TimeSeriesUnkownFailuresChart extends TimeSeriesChart {

    private static final String Y_AXIS_LABEL = "Percent unknown failure causes";
    private static final int HUNDRED_PERCENT = 100;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph, set to null for non-project graphs
     * @param filter the filter used when fetching data for this graph
     * @param intervalSize the interval sizes in which the data is grouped
     * @param graphTitle The title of the graph
     */
    public TimeSeriesUnkownFailuresChart(long timestamp, int defaultW, int defaultH, AbstractProject project,
            GraphFilterBuilder filter, int intervalSize, String graphTitle) {
        super(timestamp, defaultW, defaultH, project, filter, intervalSize, false, graphTitle);
    }

    @Override
    protected JFreeChart createGraph() {
        TimeTableXYDataset dataset = createDataset();

        ValueAxis xAxis = new DateAxis();
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);

        Calendar lowerBound = getLowerGraphBound();
        xAxis.setRange(lowerBound.getTimeInMillis(), Calendar.getInstance().getTimeInMillis());

        NumberAxis yAxis = new NumberAxis(Y_AXIS_LABEL);
        yAxis.setRange(0, HUNDRED_PERCENT);

        XYItemRenderer renderer = new XYBarRenderer();

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

        JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.removeLegend();

        return chart;
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    private TimeTableXYDataset createDataset() {
        TimeTableXYDataset dataset = new TimeTableXYDataset();
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();

        Map<TimePeriod, Double> periodQuotas = knowledgeBase.getUnknownFailureCauseQuotaPerTime(intervalSize, filter);

        for (Entry<TimePeriod, Double> entry : periodQuotas.entrySet()) {
            dataset.add(entry.getKey(), entry.getValue() * HUNDRED_PERCENT, "");
        }
        return dataset;

    }

}
