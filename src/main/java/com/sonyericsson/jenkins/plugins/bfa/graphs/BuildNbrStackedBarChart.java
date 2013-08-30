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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;

import hudson.model.AbstractProject;

/**
 * Stacked bar chart displaying the number of different failure causes for every build number.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public class BuildNbrStackedBarChart extends StackedBarChart {
    private static final String X_AXIS_TITLE = "Build number";
    private static final String Y_AXIS_TITLE = "Number of failures";

    private int nbrOfBuildsToShow;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph, set to null for non-project graphs
     * @param filter the filter used when fetching data for this graph
     * @param nbrOfBuildsToShow maximum number of builds to show
     * @param graphTitle The title of the graph
     */
    protected BuildNbrStackedBarChart(long timestamp, int defaultW,
            int defaultH, AbstractProject project, GraphFilterBuilder filter,
            int nbrOfBuildsToShow, String graphTitle) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
        this.nbrOfBuildsToShow = nbrOfBuildsToShow;
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    @Override
    protected DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<Integer> latestBuildNumbers = BFAGraph.getBuildNumbers(project);
        Collections.reverse(latestBuildNumbers);
        int nbrOfBuilds = latestBuildNumbers.size();
        if (nbrOfBuilds > nbrOfBuildsToShow) {
            latestBuildNumbers = latestBuildNumbers.subList(0, nbrOfBuildsToShow);
        }
        filter.setBuildNumbers(latestBuildNumbers);

        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        Map<Integer, List<FailureCause>> map = knowledgeBase.getFailureCausesPerBuild(filter);
        if (map != null) {
            for (int buildNumber : latestBuildNumbers) {
                String buildNumberString = "#" + buildNumber;
                List<FailureCause> failureCauses = map.get(buildNumber);
                if (failureCauses == null) {
                    dataset.addValue(0, NO_FAILURE, buildNumberString);
                } else {
                    for (FailureCause cause : failureCauses) {
                        dataset.addValue(1, cause.getName(), buildNumberString);
                    }
                }
            }
        }
        return dataset;
    }

    @Override
    protected String getTitle() {
        return graphTitle;
    }

    @Override
    protected String getYAxisTitle() {
        return Y_AXIS_TITLE;
    }

    @Override
    protected String getXAxisTitle() {
        return X_AXIS_TITLE;
    }

}
