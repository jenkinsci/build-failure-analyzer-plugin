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
package com.sonyericsson.jenkins.plugins.bfa;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.kohsuke.stapler.StaplerRequest;
import com.sonyericsson.jenkins.plugins.bfa.graphs.GraphCache;
import com.sonyericsson.jenkins.plugins.bfa.graphs.GraphType;

import hudson.model.ModelObject;
import hudson.model.RootAction;
import hudson.util.Graph;


/**
 * Abstract class to handle the detailed graphs pages.
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public abstract class BfaGraphAction implements RootAction {
    /**
     * Url-parameter for indicating time period to show in a graph.
     */
    protected static final String URL_PARAM_TIME_PERIOD = "time";
    /**
     * Url-parameter for indicating which graph to show.
     */
    protected static final String URL_PARAM_WHICH_GRAPH = "which";
    /**
     * Url-parameter for indicating whether to show or hide aborted builds.
     */
    protected static final String URL_PARAM_SHOW_ABORTED = "showAborted";

    /**
     * Url-parameter for indicating whether to show for all masters or not.
     */
    protected static final String URL_PARAM_ALL_MASTERS = "allMasters";

    /**
     * Url-parameter value for 'today'.
     */
    protected static final String URL_PARAM_VALUE_TODAY = "today";

    /**
     * Url-parameter value for 'month'.
     */
    protected static final String URL_PARAM_VALUE_MONTH = "month";

    /**
     * Url-parameter value for 'max'.
     */
    protected static final String URL_PARAM_VALUE_MAX = "max";

    /**
     * Default width for graphs on detail pages.
     */
    protected static final int DEFAULT_GRAPH_WIDTH = 700;

    /**
     * Default height for graphs on detail pages.
     */
    protected static final int DEFAULT_GRAPH_HEIGHT = 500;

    /**
     * Constant for "ABORTED"-cause (used to exclude such {@link com.sonyericsson.jenkins.plugins.bfa.model.FailureCause}s).
     */
    protected static final String EXCLUDE_ABORTED = "ABORTED";

    /**
     * Separator between different parts of graph IDs.
     */
    protected static final char ID_SEPARATOR = '-';

    /**
     * Get the owner.
     * @return The owner
     */
    public abstract ModelObject getOwner();

    /**
     * Returns an array of {@link GraphType}s, where each element represents
     * a graph. These are the types used to display the graphs/images
     * on the detailed graphs page, that is, they will be the 'which'-parameter
     * to getGraph(GraphType which, Date ...).
     * The graphs are displayed in the same order as the numbers in the array.
     * @return An array of {@link GraphType}s where each element
     * represents a graph to display
     */
    public abstract GraphType[] getGraphTypes();

    /**
     * Get the title to display in the top of the detailed graphs page.
     * @return The title as a String
     */
    public abstract String getGraphsPageTitle();

    /**
     * Get the graph corresponding to the specified arguments.
     * @param which Which graph to display
     * @param timePeriod How old statistics should be included in the graph
     * @param hideManAborted Hide manually aborted causes
     * @param allMasters Show for all masters
     * @param rawReqParams The url parameters that came with the request
     * @return A Graph
     */
    protected abstract Graph getGraph(GraphType which, Date timePeriod,
            boolean hideManAborted, boolean allMasters,
            Map<String, String> rawReqParams);

    /**
     * Get the Graph corresponding to the url-parameters.
     * Parameters:
     * - time : how far back should statistics be included
     * - which : which graph to display
     * - showAborted : show manually aborted
     * - allMasters : show for all masters
     * @param req The StaplerRequest
     * @return A graph
     */
    public Graph getGraph(StaplerRequest req) {
        final Map<String, String> rawReqParams = new HashMap<String, String>();

        String reqTimePeriod = req.getParameter(URL_PARAM_TIME_PERIOD);
        if (reqTimePeriod == null || !reqTimePeriod.matches(URL_PARAM_VALUE_MONTH + "|" + URL_PARAM_VALUE_MAX)) {
            reqTimePeriod = URL_PARAM_VALUE_TODAY; // The default value
        }
        rawReqParams.put(URL_PARAM_TIME_PERIOD, reqTimePeriod);

        String reqWhich = req.getParameter(URL_PARAM_WHICH_GRAPH);
        rawReqParams.put(URL_PARAM_WHICH_GRAPH, reqWhich);

        String showAborted = req.getParameter(URL_PARAM_SHOW_ABORTED);
        rawReqParams.put(URL_PARAM_SHOW_ABORTED, showAborted);

        String allMasters = req.getParameter(URL_PARAM_ALL_MASTERS);
        rawReqParams.put(URL_PARAM_ALL_MASTERS, allMasters);

        final Date sinceDate = getDateForUrlStr(reqTimePeriod);
        int tmpWhichGraph =  -1;
        try {
            tmpWhichGraph = Integer.parseInt(reqWhich);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        final GraphType whichGraph = GraphType.toEnum(tmpWhichGraph);
        final boolean hideAborted = "0".equals(showAborted);
        final boolean forAllMasters = "1".equals(allMasters);

        String id = getGraphCacheId(whichGraph, reqTimePeriod, hideAborted, forAllMasters);
        Graph graphToReturn = null;
        try {
            graphToReturn = GraphCache.getInstance().get(id, new Callable<Graph>() {
                @Override
                public Graph call() throws Exception {
                    // The requested graph isn't cached, so create a new one.
                    Graph g = getGraph(whichGraph, sinceDate, hideAborted, forAllMasters, rawReqParams);
                    if (g != null) {
                        return g;
                    }
                    // According to documentation, null must not be returned; either
                    // a non-null value must be returned, or an an exception thrown
                    throw new ExecutionException("Graph-parameters not valid", null);
                } });
        } catch (ExecutionException e) {
            // An exception will occur when a graph cannot be generated,
            // e.g. when erroneous url-parameters have been specified
            e.printStackTrace();
        }
        return graphToReturn;
    }

    /**
     * Get a unique id used in the caching of the graph.
     * @param whichGraph Which graph
     * @param reqTimePeriod The selected time period
     * @param hideAborted Hide aborted builds
     * @param forAllMasters For all masters
     * @return An id corresponding to the specified arguments
     */
    protected abstract String getGraphCacheId(GraphType whichGraph,
            String reqTimePeriod, boolean hideAborted, boolean forAllMasters);

    /**
     * Helper for groovy-views; Get the default width of graphs on detailed pages.
     * @return The default height of graphs on the detailed graphs-page
     */
    public int getDefaultGraphWidth() {
        return DEFAULT_GRAPH_WIDTH;
    }

    /**
     * Helper for groovy-views; Get the default height of graphs on detailed pages.
     * @return The default height of graphs on the detailed graphs-page
     */
    public int getDefaultGraphHeight() {
        return DEFAULT_GRAPH_HEIGHT;
    }

    /**
     * Helper for the groovy-views; show/hide Masters-switch.
     * Whether to show links for switching between all masters
     * and the own master.
     * @return True to show the switch, otherwise false
     */
    public boolean showMasterSwitch() {
        return false;
    }

    /**
     * Helper for the groovy-views; show/hide info text for graph delay.
     * The info text will inform the user about the delay for graphs
     * because of caching.
     * @return True to show the text, otherwise false
     */
    public boolean showGraphDelayText() {
        return false;
    }

    /**
     * Get a Date object corresponding to the specified string.
     * (today|month).
     * @param str The String
     * @return A Date, or null if not equal to "today" or "month"
     */
    private Date getDateForUrlStr(String str) {
        Calendar cal = Calendar.getInstance();
        Date date = null;
        if (URL_PARAM_VALUE_TODAY.equals(str)) {
           cal.add(Calendar.DAY_OF_YEAR, -1);
           date = cal.getTime();
        } else if (URL_PARAM_VALUE_MONTH.equals(str)) {
            cal.add(Calendar.MONTH, -1);
            date = cal.getTime();
        } // max time => return null
        return date;
     }
}
