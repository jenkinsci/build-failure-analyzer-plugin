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

import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

/**
 * Bar chart displaying the number of different failure causes for a project.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class BarChart extends BFAGraph {

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph
     * @param filter the filter used when fetching data for this graph
     * @param graphTitle The title of the graph
     */
    public BarChart(long timestamp, int defaultW, int defaultH,
            AbstractProject project, GraphFilterBuilder filter,
            String graphTitle) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
    }

    @Override
    protected JFreeChart createGraph() {
        CategoryDataset dataset = createDataset();
        JFreeChart chart = ChartFactory.createBarChart(graphTitle, "", "Number of failures", dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        NumberAxis domainAxis = new NumberAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setRangeAxis(domainAxis);
        return chart;
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    private CategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        List<ObjectCountPair<FailureCause>> nbrOfFailureCauses = knowledgeBase.getNbrOfFailureCauses(filter);

        int othersCount = 0;
        for (int i = 0; i < nbrOfFailureCauses.size(); i++) {
            ObjectCountPair<FailureCause> countPair = nbrOfFailureCauses.get(i);
            if (i < MAX_GRAPH_ELEMENTS) {
                dataset.setValue(countPair.getCount(), "", countPair.getObject().getName());
            } else {
                othersCount += countPair.getCount();
            }
        }
        if (othersCount > 0) {
            dataset.setValue(othersCount, "", GRAPH_CAT_OTHERS);
        }

        long nullFailureCauses = knowledgeBase.getNbrOfNullFailureCauses(filter);
        if (nullFailureCauses > 0) {
            dataset.addValue(nullFailureCauses, "", "NO FAILURE CAUSE");
        }
        return dataset;
    }

}
