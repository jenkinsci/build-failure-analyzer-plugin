package com.sonyericsson.jenkins.plugins.bfa;

import java.util.Calendar;
import java.util.Date;

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
     * Constant for bar chart with failure causes.
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
     * Constant for pie chart with failure causes.
     */
    protected static final int PIE_CHART_CAUSES = 5;

    /**
     * Constant for pie chart with categories.
     */
    protected static final int PIE_CHART_CATEGORIES = 6;

    /**
     * Get the owner.
     * @return The owner
     */
    public abstract ModelObject getOwner();

    /**
     * Returns an array of numbers, where each number represents
     * a graph. These are the numbers used to display the graphs/images
     * on the detailed graphs page, that is, they will be parameters
     * to getGraph(int which, Date ...).
     * The graphs are displayed in same order as the numbers in the array.
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
     * @return A Graph
     */
    protected abstract Graph getGraph(int which, Date timePeriod, boolean hideManAborted);

    /**
     * Get the Graph corresponding to the url-parameters;
     * - time : how far back should statistics be included
     * - which : which graph to display
     * - showAborted : show manually aborted.
     * @param req The StaplerRequest
     * @return A graph
     */
    public Graph getGraph(StaplerRequest req) {
        String reqTimePeriod = req.getParameter(URL_PARAM_TIME_PERIOD);
        String reqWhich = req.getParameter(URL_PARAM_WHICH_GRAPH);
        String showAborted = req.getParameter(URL_PARAM_SHOW_ABORTED);
        Date sinceDate = getDateForUrlStr(reqTimePeriod);
        int whichGraph =  -1;
        try {
            whichGraph = Integer.parseInt(reqWhich);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        boolean hideAborted = "0".equals(showAborted);
        // TODO: check cache
        return getGraph(whichGraph, sinceDate, hideAborted);
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
     * Get a Date object corresponding to the specified string.
     * (today|month|all).
     * @param str The String
     * @return A Date
     */
    private Date getDateForUrlStr(String str) {
        Calendar cal = Calendar.getInstance();
        Date date = null;
        if ("today".equals(str)) {
           cal.add(Calendar.DAY_OF_YEAR, -1);
           date = cal.getTime();
        } else if ("month".equals(str)) {
            cal.add(Calendar.MONTH, -1);
            date = cal.getTime();
        } // all-time => pass in null
        return date;
     }
}
