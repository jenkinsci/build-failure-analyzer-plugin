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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import com.sonyericsson.jenkins.plugins.bfa.BfaGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;

import hudson.model.ModelObject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.util.Graph;

/**
 * Class for displaying graphs for nodes - slaves/masters.
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public class ComputerGraphAction extends BfaGraphAction {
    /**
     * Title for graphs with failure causes
     */
    private static final String GRAPH_TITLE_CAUSES = "Failure causes for this node";

    /**
     * Title for graphs with categories
     */
    private static final String GRAPH_TITLE_CATEGORIES = "Failures grouped by categories for this node";

    /**
     * The url-name of the Action
     */
    private static final String URL_NAME = "bfa-comp-graphs";

    /**
     * The display-name of the action
     */
    private static final String DISPLAY_NAME = "Graph statistics";

    private Computer computer;

    /**
     * Standard constructor.
     * @param computer The computer/node
     */
    public ComputerGraphAction(Computer computer) {
        this.computer = computer;
    }

    @Override
    public String getIconFileName() {
        if (Hudson.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)
                && PluginImpl.getInstance().isGraphsEnabled()) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (PluginImpl.getInstance().isGraphsEnabled()) {
            return DISPLAY_NAME;
        }
        return null;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    /**
     * Get the name of the node.
     * @return The name as a String, or null
     */
    private String getNodeName() {
        if (computer == null) {
            return null;
        }
        return computer.getName();
    }

    /**
     * Returns whether the node is a slave or master
     * @return True if a slave, otherwise false
     */
    private boolean isSlave() {
        if (computer == null) {
            return false;
        }
        return (computer.getNode() instanceof Slave);
    }

    @Override
    public ModelObject getOwner() {
        return computer;
    }

    @Override
    public GraphType[] getGraphTypes() {
        return new GraphType[] { GraphType.BAR_CHART_CAUSES, GraphType.PIE_CHART_CAUSES,
                GraphType.TIME_SERIES_CHART_CAUSES, GraphType.BAR_CHART_CATEGORIES,
                GraphType.PIE_CHART_CATEGORIES, GraphType.TIME_SERIES_CHART_CATEGORIES, };
    }

    @Override
    public String getGraphsPageTitle() {
        return "Statistics for node " + getNodeName();
    }

    @Override
    protected Graph getGraph(GraphType which, Date timePeriod,
            boolean hideManAborted, boolean forAllMasters,
            Map<String, String> rawReqParams) {
        GraphFilterBuilder filter = getDefaultBuilder(hideManAborted,
                timePeriod);
        switch (which) {
        case BAR_CHART_CAUSES:
            return new BarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    null, filter, GRAPH_TITLE_CAUSES, false);
        case BAR_CHART_CATEGORIES:
            return new BarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    null, filter, GRAPH_TITLE_CATEGORIES, true);
        case PIE_CHART_CAUSES:
            return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    null, filter, GRAPH_TITLE_CAUSES, false);
        case PIE_CHART_CATEGORIES:
            return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    null, filter, GRAPH_TITLE_CATEGORIES, true);
        case TIME_SERIES_CHART_CAUSES:
            return getTimeSeriesChart(false, GRAPH_TITLE_CAUSES, filter,
                    rawReqParams);
        case TIME_SERIES_CHART_CATEGORIES:
            return getTimeSeriesChart(true, GRAPH_TITLE_CATEGORIES, filter,
                    rawReqParams);
        default:
            break;
        }
        return null;
    }

    /**
     * Get a time series chart corresponding to the specified arguments.
     * @param byCategories True to group by categories, or false causes
     * @param title The title of the graph
     * @param filter GraphFilterBuilder to specify data to use
     * @param rawReqParams A map with the url-parameters from the request
     * @return A time series graph
     */
    private Graph getTimeSeriesChart(boolean byCategories, String title,
            GraphFilterBuilder filter, Map<String, String> rawReqParams) {
        String date = rawReqParams.get(URL_PARAM_TIME_PERIOD);

        int interval = 0;
        Calendar cal = Calendar.getInstance();
        if (URL_PARAM_VALUE_TODAY.equals(date)) {
            interval = Calendar.HOUR_OF_DAY;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else {
            interval = Calendar.DATE;
            cal.add(Calendar.MONTH, -1);
        }
        filter.setSince(cal.getTime());
        return new TimeSeriesChart(-1, DEFAULT_GRAPH_WIDTH,
                DEFAULT_GRAPH_HEIGHT, null, filter, interval, byCategories,
                title);
    }

    /**
     * Get a GraphFilterBuilder corresponding to the specified arguments.
     * @param hideAborted Hide manually aborted
     * @param period The time period
     * @return A GraphFilterBuilder
     */
    private GraphFilterBuilder getDefaultBuilder(boolean hideAborted, Date period) {
        GraphFilterBuilder filter = new GraphFilterBuilder();
        if (hideAborted) {
            filter.setExcludeResult(EXCLUDE_ABORTED);
        }
        filter.setSince(period);
        String nodeName = getNodeName();
        if (isSlave()) {
            filter.setSlaveName(nodeName);
        } else {
            // Computer.getName() returns empty string for master,
            // so let's get the name the other way
            filter.setMasterName(BfaUtils.getMasterName());
        }
        return filter;
    }

    @Override
    protected String getGraphCacheId(GraphType whichGraph, String reqTimePeriod,
            boolean hideAborted, boolean forAllMasters) {
        return getClass().getSimpleName() + ID_SEPARATOR
                + whichGraph.getValue() + ID_SEPARATOR
                + getNodeName() + ID_SEPARATOR
                + reqTimePeriod + ID_SEPARATOR
                + String.valueOf(hideAborted);
    }

    /**
     * Invalidate all graph caches for the specified computer.
     * @param computer The computer whose graphs to invalidate
     */
    public static void invalidateNodeGraphCache(Computer computer) {
        if (computer != null) {
            Pattern projectPattern = Pattern.compile("^.*" + ID_SEPARATOR
                    + computer.getName() + ID_SEPARATOR + ".*$");
            GraphCache.getInstance().invalidateMatching(projectPattern);
        }
    }

    /**
     * Invalidate all graph caches for the specified buildNode.
     * @param buildNode The buildNode whose graphs to invalidate
     */
    public static void invalidateNodeGraphCache(Node buildNode) {
        if (buildNode != null) {
            Computer correspondingComputer = buildNode.toComputer();
            invalidateNodeGraphCache(correspondingComputer);
        }
    }
}
