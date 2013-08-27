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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

/**
 * Bar chart displaying the number of different failure causes for a project.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class TimeSeriesChart extends BFAGraph {

    private static final String Y_AXIS_LABEL = "Number";
    private static final int LIMIT_BEFORE_GROUPING = 8;

    private int intervalSize;
    private boolean groupByCategories;
    private Map<TimePeriod, List<FailureCauseTimeInterval>> excludedDataForPeriod;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph, set to null for non-project graphs
     * @param filter the filter used when fetching data for this graph
     * @param intervalSize the interval sizes in which the data is grouped
     * @param groupByCategories set to true in order to group failure causes by their categories
     * @param graphTitle The title of the graph
     */
    public TimeSeriesChart(long timestamp, int defaultW, int defaultH,
            AbstractProject project, GraphFilterBuilder filter,
            int intervalSize, boolean groupByCategories, String graphTitle) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
        this.intervalSize = intervalSize;
        this.groupByCategories = groupByCategories;
    }

    @Override
    protected JFreeChart createGraph() {
        TimeTableXYDataset dataset = createDataset();
        ValueAxis xAxis = new DateAxis();
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        NumberAxis yAxis = new NumberAxis(Y_AXIS_LABEL);

        StackedXYBarRenderer renderer = new StackedXYBarRenderer();

        renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                String seriesKey = dataset.getSeriesKey(series).toString();
                StringBuilder sb = new StringBuilder();

                if (seriesKey.equals(GRAPH_OTHERS)) {
                    long timeInMillis = dataset.getX(series, item).longValue();
                    Date time = new Date(timeInMillis);

                    TimePeriod period = null;
                    if (intervalSize == Calendar.DATE) {
                        period = new Day(time);
                    } else if (intervalSize == Calendar.HOUR_OF_DAY) {
                        period = new Hour(time);
                    }
                    List<FailureCauseTimeInterval> excludedDataList = excludedDataForPeriod.get(period);
                    Collections.sort(excludedDataList, new Comparator<FailureCauseTimeInterval>() {

                        @Override
                        public int compare(FailureCauseTimeInterval o1, FailureCauseTimeInterval o2) {
                            return o2.getNumber() - o1.getNumber();
                        }
                    });

                    for (FailureCauseTimeInterval excludedData : excludedDataList) {
                        sb.append(excludedData).append(" \n");
                    }
                } else {
                    int number = dataset.getY(series, item).intValue();
                    sb.append(seriesKey).append(": ").append(number);
                }
                return sb.toString();
            }
        });

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);

        plot.setRangeAxis(yAxis);

        JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        return chart;
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    private TimeTableXYDataset createDataset() {
        TimeTableXYDataset dataset = new TimeTableXYDataset();
        excludedDataForPeriod = new HashMap<TimePeriod, List<FailureCauseTimeInterval>>();
        Set<String> topItems = new HashSet<String>();

        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();

        List<ObjectCountPair<String>> topEntries;
        if (groupByCategories) {
            topEntries = knowledgeBase.getNbrOfFailureCategoriesPerName(filter, LIMIT_BEFORE_GROUPING);
        } else {
            topEntries = knowledgeBase.getNbrOfFailureCausesPerId(filter, LIMIT_BEFORE_GROUPING);
        }

        if (topEntries != null) {
            for (ObjectCountPair<String> countPair : topEntries) {
                topItems.add(countPair.getObject());
            }
        }

        Collection<FailureCauseTimeInterval> intervals = knowledgeBase.getFailureCausesPerTime(intervalSize, filter,
                groupByCategories);

        for (FailureCauseTimeInterval failureInterval : intervals) {
            TimePeriod period = failureInterval.getPeriod();
            String name = failureInterval.getName();
            int number = failureInterval.getNumber();


            if (topItems.contains(failureInterval.getId()) || topItems.contains(name)) {
                dataset.add(period, number, name);
            } else {
                // Smaller, needs grouping
                List<FailureCauseTimeInterval> list = excludedDataForPeriod.get(period);
                if (list == null) {
                    list = new ArrayList<FailureCauseTimeInterval>();
                    excludedDataForPeriod.put(period, list);
                }
                list.add(new FailureCauseTimeInterval(period, name, number));
            }

        }
        // Create OTHERS-bars for all excluded data
        for (Entry<TimePeriod, List<FailureCauseTimeInterval>> entry : excludedDataForPeriod.entrySet()) {
            TimePeriod period = entry.getKey();
            List<FailureCauseTimeInterval> list = entry.getValue();
            int sum = 0;
            for (FailureCauseTimeInterval excludedData : list) {
                sum += excludedData.getNumber();
            }
            dataset.add(period, sum, GRAPH_OTHERS);
        }
        return dataset;
    }

}
