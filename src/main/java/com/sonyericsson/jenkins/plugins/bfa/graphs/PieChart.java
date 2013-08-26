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
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

/**
 * Pie chart displaying the distribution of failure causes for a project.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class PieChart extends BFAGraph {
    private boolean byCategories;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph
     * @param filter the filter used when fetching data for this graph
     * @param graphTitle The title of the graph
     * @param byCategories True to display categories, or false for failure causes
     */
    public PieChart(long timestamp, int defaultW, int defaultH,
            AbstractProject project, GraphFilterBuilder filter,
            String graphTitle, boolean byCategories) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
        this.byCategories = byCategories;
    }

    @Override
    protected JFreeChart createGraph() {
        PieDataset dataset = createDataset();
        return ChartFactory.createPieChart(graphTitle, dataset, true, true, false);
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    private PieDataset createDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        List<ObjectCountPair<String>> nbrOfFailureCauses = null;
        if (byCategories) {
            nbrOfFailureCauses = knowledgeBase.getNbrOfFailureCategoriesPerName(filter, -1);
        } else {
            nbrOfFailureCauses = knowledgeBase.getFailureCauseNames(filter);
        }

        int othersCount = 0;
        for (int i = 0; i < nbrOfFailureCauses.size(); i++) {
            ObjectCountPair<String> countPair = nbrOfFailureCauses.get(i);
            if (i < MAX_GRAPH_ELEMENTS) {
                dataset.setValue(countPair.getObject(), countPair.getCount());
            } else {
                othersCount += countPair.getCount();
            }
        }
        if (othersCount > 0) {
            dataset.setValue(GRAPH_OTHERS, othersCount);
        }
        return dataset;
    }

}
