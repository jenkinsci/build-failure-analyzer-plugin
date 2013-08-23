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
//    private static final int GRAPH_WIDTH = 700;
//    private static final int GRAPH_HEIGHT = 500;
//
    private static final int BAR_CHART_CAUSES_SMALL = 1;
//    private static final int BAR_CHART_CAUSES = 2;
//    private static final int BAR_CHART_CATEGORIES = 3;
//    private static final int BAR_CHART_BUILD_NBRS = 4;
//    private static final int PIE_CHART_CAUSES = 5;
//    private static final int PIE_CHART_CATEGORIES = 6;

    private AbstractProject project;

    // private static final int GRAPH_WIDTH = 500;
    // private static final int GRAPH_HEIGHT = 200;

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
        return new int[] {BAR_CHART_CAUSES_SMALL};
    }

    @Override
    public String getGraphsPageTitle() {
        return "Statistics for project";
    }

    @Override
    protected Graph getGraph(int which, Date timePeriod, boolean hideManAborted) {
        switch(which) {
        case BAR_CHART_CAUSES_SMALL:
            return getBarChart(true, GRAPH_WIDTH_SMALL, GRAPH_HEIGHT_SMALL,
                    timePeriod, hideManAborted);
//        case BAR_CHART_CAUSES: return getGraph1();
//        case BAR_CHART_CATEGORIES: return getGraph2();
//        case BAR_CHART_BUILD_NBRS: return getGraph3();
//        case PIE_CHART_CAUSES: return getGraph4();
//        case PIE_CHART_CATEGORIES: return getGraph5();
        default: break;
        }
        return null;
    }

    /**
     * Get a bar chart according to the arguments
     * @param byCauses Display causes (true) or categories (false)
     * @param width The with of the graph
     * @param height The height of the graph
     * @param period The time period
     * @param hideAborted Hide aborted
     * @return A graph
     */
    private Graph getBarChart(boolean byCauses, int width, int height, Date period, boolean hideAborted) {
        GraphFilterBuilder filter = new GraphFilterBuilder();
        filter.setProjectName(project.getFullName());
        if (hideAborted) {
            filter.setExcludeResult("ABORTED");
        }
        filter.setSince(period);
        return new BarChart(-1, width, height, project, filter);
    }
    // /**
    // * Gets the first project graph.
    // * @return project graph
    // */
    // public Graph getGraph() {
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // filter.setProjectName(project.getFullName());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new PieChart(-1, GRAPH_WIDTH, GRAPH_HEIGHT, project, filter);
    // }
    //
    // /**
    // * Gets the second project graph.
    // * @return project graph
    // */
    // public Graph getGraph2() {
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // filter.setProjectName(project.getFullName());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new BarChart(-1, GRAPH_WIDTH, GRAPH_HEIGHT, project, filter);
    // }
    //
    // /**
    // * Gets the third project graph.
    // * @return project graph
    // */
    // public Graph getGraph3() {
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // filter.setProjectName(project.getFullName());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new BuildNbrStackedBarChart(-1, GRAPH_WIDTH, GRAPH_HEIGHT,
    // project, filter, 25);
    // }
    //
    // public Graph getGraph4() {
    // Calendar yesterday = Calendar.getInstance();
    // yesterday.add(Calendar.DATE, -11);
    //
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // // filter.setProjectName(project.getFullName());
    // filter.setSince(yesterday.getTime());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new TimeSeriesChart(-1, 800, 700, project, filter,
    // Calendar.HOUR_OF_DAY, true);
    // }
    //
    // public Graph getGraph5() {
    // Calendar yesterday = Calendar.getInstance();
    // yesterday.add(Calendar.DATE, -11);
    //
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // // filter.setProjectName(project.getFullName());
    // filter.setSince(yesterday.getTime());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new TimeSeriesChart(-1, 800, 700, project, filter,
    // Calendar.HOUR_OF_DAY, false);
    // }
    //
    // public Graph getGraph6() {
    // Calendar yesterday = Calendar.getInstance();
    // yesterday.add(Calendar.DATE, -41);
    //
    // GraphFilterBuilder filter = new GraphFilterBuilder();
    // // filter.setProjectName(project.getFullName());
    // filter.setSince(yesterday.getTime());
    // filter.setExcludeResult("ABORTED");
    // // filter.setMasterName(BfaUtils.getMasterName());
    // return new TimeSeriesChart(-1, 800, 700, project, filter, Calendar.DATE,
    // false);
    // }


}
