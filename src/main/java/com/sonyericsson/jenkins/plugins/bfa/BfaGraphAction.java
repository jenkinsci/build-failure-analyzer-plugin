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
     * Get a Date object corresponding to the specified string
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
