package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.Date;

import com.sonyericsson.jenkins.plugins.bfa.BfaGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;

import hudson.model.ModelObject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Slave;
import hudson.util.Graph;

/**
 * Class for displaying graphs for node, slave/master.
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public class ComputerGraphAction extends BfaGraphAction {
    /**
     * Title for graphs with failure causes
     */
    private static final String GRAPH_TITLE_CAUSES = "Total failure causes for this node";

    /**
     * Title for graphs with categories
     */
    private static final String GRAPH_TITLE_CATEGORIES = "Failures per category for this node";

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
        // TODO: Return an other icon?
        if (Hudson.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
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
        // TODO: reliable check?
        return (computer.getNode() instanceof Slave);
    }

    @Override
    public ModelObject getOwner() {
        return computer;
    }

    @Override
    public int[] getGraphNumbers() {
        return new int[] { BAR_CHART_CAUSES, PIE_CHART_CAUSES,
                BAR_CHART_CATEGORIES, PIE_CHART_CATEGORIES, };
    }

    @Override
    public String getGraphsPageTitle() {
        return "Statistics for node " + getNodeName();
    }

    @Override
    public Graph getGraph(int which, Date timePeriod, boolean hideManAborted, boolean forAllMasters) {
        switch (which) {
        case BAR_CHART_CAUSES: return getBarChart(false, timePeriod, hideManAborted, GRAPH_TITLE_CAUSES);
        case BAR_CHART_CATEGORIES: return getBarChart(true, timePeriod, hideManAborted, GRAPH_TITLE_CATEGORIES);
        case PIE_CHART_CAUSES: return getPieChart(false, timePeriod, hideManAborted, GRAPH_TITLE_CAUSES);
        case PIE_CHART_CATEGORIES: return getPieChart(true, timePeriod, hideManAborted, GRAPH_TITLE_CATEGORIES);
        default: return null;
        }
    }

    /**
     * Get a pie chart according to the specified arguments.
     * @param byCategories True to display categories, or false to display causes
     * @param period The time period
     * @param hideAborted Hide manually aborted
     * @param title The title of the graph
     * @return A graph
     */
    private Graph getPieChart(boolean byCategories, Date period,
            boolean hideAborted, String title) {
        GraphFilterBuilder filter = getDefaultBuilder(hideAborted, period);
        return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                null, filter, title, byCategories);
    }

    /**
     * Get a bar chart according to the specified arguments.
     * @param byCategories True to display categories, or false to display causes
     * @param period The time period
     * @param hideAborted Hide manually aborted
     * @param title The title of the graph
     * @return A graph
     */
    private Graph getBarChart(boolean byCategories, Date period, boolean hideAborted, String title) {
        GraphFilterBuilder filter = getDefaultBuilder(hideAborted, period);
        return new BarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT, null, filter, title, byCategories);
    }

    /**
     * Get a GraphFilterBuilder with the specified arguments, and
     * the slave- or master-name set.
     * @param hideAborted Hide manually aborted
     * @param period The time period
     * @return A graphFilterBuilder
     */
    private GraphFilterBuilder getDefaultBuilder(boolean hideAborted, Date period) {
        GraphFilterBuilder filter = new GraphFilterBuilder();
        if (hideAborted) {
            filter.setExcludeResult("ABORTED");
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
}
