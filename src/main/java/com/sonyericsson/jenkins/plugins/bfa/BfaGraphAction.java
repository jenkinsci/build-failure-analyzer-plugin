package com.sonyericsson.jenkins.plugins.bfa;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;
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
     * Default width for graphs on detail pages.
     */
    protected static final int DEFAULT_GRAPH_WIDTH = 700;

    /**
     * Default height for graphs on detail pages.
     */
    protected static final int DEFAULT_GRAPH_HEIGHT = 500;

    /**
     * Constant for small bar chart.
     */
    protected static final int BAR_CHART_CAUSES_SMALL = 1;

    /**
     * Constant for bar chart with {@link FailureCause}s.
     */
    protected static final int BAR_CHART_CAUSES = 2;

    /**
     * Constant for bar chart with categories.
     */
    protected static final int BAR_CHART_CATEGORIES = 3;

    /**
     * Constant for bar chart with build numbers.
     */
    protected static final int BAR_CHART_BUILD_NBRS = 4;

    /**
     * Constant for pie chart with {@link FailureCause}s.
     */
    protected static final int PIE_CHART_CAUSES = 5;

    /**
     * Constant for pie chart with categories.
     */
    protected static final int PIE_CHART_CATEGORIES = 6;

    /**
     * Constant for time series chart with {@link FailureCause}s.
     */
    protected static final int TIME_SERIES_CHART_CAUSES = 7;

    /**
     * Constant for time series chart with categories.
     */
    protected static final int TIME_SERIES_CHART_CATEGORIES = 8;

    /**
     * Constant for "ABORTED"-cause (used to exclude such {@link FailureCause}s).
     */
    protected static final String EXCLUDE_ABORTED = "ABORTED";
    /**
     * Get the owner.
     * @return The owner
     */
    public abstract ModelObject getOwner();

    /**
     * Returns an array of numbers, where each number represents
     * a graph. These are the numbers used to display the graphs/images
     * on the detailed graphs page, that is, they will be the 'which'-parameter
     * to getGraph(int which, Date ...).
     * The graphs are displayed in the same order as the numbers in the array.
     * @return An array of integers where each integer represents a graph to
     * display
     */
    public abstract int[] getGraphNumbers();

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
    protected abstract Graph getGraph(int which, Date timePeriod,
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
        Map<String, String> rawReqParams = new HashMap<String, String>();

        String reqTimePeriod = req.getParameter(URL_PARAM_TIME_PERIOD);
        rawReqParams.put(URL_PARAM_TIME_PERIOD, reqTimePeriod);

        String reqWhich = req.getParameter(URL_PARAM_WHICH_GRAPH);
        rawReqParams.put(URL_PARAM_WHICH_GRAPH, reqWhich);

        String showAborted = req.getParameter(URL_PARAM_SHOW_ABORTED);
        rawReqParams.put(URL_PARAM_SHOW_ABORTED, showAborted);

        String allMasters = req.getParameter(URL_PARAM_ALL_MASTERS);
        rawReqParams.put(URL_PARAM_ALL_MASTERS, allMasters);

        Date sinceDate = getDateForUrlStr(reqTimePeriod);
        int whichGraph =  -1;
        try {
            whichGraph = Integer.parseInt(reqWhich);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        boolean hideAborted = "0".equals(showAborted);
        boolean forAllMasters = "1".equals(allMasters);
        // TODO: check cache
        return getGraph(whichGraph, sinceDate, hideAborted, forAllMasters, rawReqParams);
    }

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
        } // all time => return null
        return date;
     }
}
