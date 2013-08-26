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

import java.util.Date;

import com.sonyericsson.jenkins.plugins.bfa.BfaGraphAction;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.util.Graph;

/**
 * Action class for displaying graphs on the project page.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class ProjectGraphAction extends BfaGraphAction {
    private static final int GRAPH_WIDTH_SMALL = 500;
    private static final int GRAPH_HEIGHT_SMALL = 200;
    private static final int NBR_OF_BUILDS = 25;

    private static final String GRAPH_TITLE_CAUSES = "Total failure causes for this project";
    private static final String GRAPH_TITLE_CATEGORIES = "Failures per category for this project";
    private static final String BUILD_NBR_TITLE = "Failure causes per build for this project";

    private AbstractProject project;

    /**
     * Standard constructor.
     * @param project the parent project of this action
     */
    public ProjectGraphAction(AbstractProject project) {
         this.project = project;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "bfa-proj-graphs";
    }

    @Override
    public ModelObject getOwner() {
        return project;
    }

    @Override
    public int[] getGraphNumbers() {
        return new int[] { BAR_CHART_CAUSES, PIE_CHART_CAUSES,
                BAR_CHART_CATEGORIES, PIE_CHART_CATEGORIES,
                BAR_CHART_BUILD_NBRS, };
    }

    @Override
    public String getGraphsPageTitle() {
        return "Statistics for project";
    }

    @Override
    protected Graph getGraph(int which, Date timePeriod, boolean hideManAborted) {
        switch (which) {
        case BAR_CHART_CAUSES_SMALL:
            return getBarChart(false, GRAPH_WIDTH_SMALL, GRAPH_HEIGHT_SMALL,
                    timePeriod, hideManAborted, GRAPH_TITLE_CAUSES);
        case BAR_CHART_CAUSES:
            return getBarChart(false, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT, timePeriod,
                    hideManAborted, GRAPH_TITLE_CAUSES);
        case BAR_CHART_CATEGORIES:
            return getBarChart(true, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT, timePeriod,
                    hideManAborted, GRAPH_TITLE_CATEGORIES);
        case BAR_CHART_BUILD_NBRS:
            return getBuildNbrChart(hideManAborted);
        case PIE_CHART_CAUSES:
            return getPieChart(false, timePeriod, hideManAborted, GRAPH_TITLE_CAUSES);
        case PIE_CHART_CATEGORIES:
            return getPieChart(true, timePeriod, hideManAborted, GRAPH_TITLE_CATEGORIES);
        default:
            break;
        }
        return null;
    }

    /**
     * Get a bar chart according to the arguments.
     * @param byCategories Display categories (true) or causes (false)
     * @param width The with of the graph
     * @param height The height of the graph
     * @param period The time period
     * @param hideAborted Hide aborted
     * @param title The title of the graph
     * @return A graph
     */
    private Graph getBarChart(boolean byCategories, int width, int height,
            Date period, boolean hideAborted, String title) {
        GraphFilterBuilder filter = getDefaultBuilder(hideAborted, period);
        return new BarChart(-1, width, height, project, filter, title,
                byCategories);
    }

    /**
     * Get a pie chart according to the specified arguments.
     * @param byCategories True to display categories, false for causes
     * @param period The time period
     * @param hideAborted Hide manually aborted
     * @param title The title of the graph
     * @return A graph
     */
    private Graph getPieChart(boolean byCategories, Date period, boolean hideAborted, String title) {
        GraphFilterBuilder filter = getDefaultBuilder(hideAborted, period);
        return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT, project, filter, title, byCategories);
    }

    /**
     * Get a build number chart.
     * @param hideAborted Hide manually aborted
     * @return A graph
     */
    private Graph getBuildNbrChart(boolean hideAborted) {
        GraphFilterBuilder filter = getDefaultBuilder(hideAborted, null);
        return new BuildNbrStackedBarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                project, filter, NBR_OF_BUILDS, BUILD_NBR_TITLE);
    }

    /**
     * Get a GraphFilterBuilder corresponding to the specified arguments, and the
     * project name set.
     * @param hideAborted Hide manually aborted
     * @param period The time period
     * @return A GraphFilterBuilder
     */
    private GraphFilterBuilder getDefaultBuilder(boolean hideAborted, Date period) {
        GraphFilterBuilder filter = new GraphFilterBuilder();
        filter.setProjectName(project.getFullName());
        if (hideAborted) {
            filter.setExcludeResult("ABORTED");
        }
        filter.setSince(period);
//        filter.setMasterName(BfaUtils.getMasterName());
        return filter;
    }
}
