package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.Date;

import com.sonyericsson.jenkins.plugins.bfa.BfaGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
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
        return new int[]{1};
    }

    @Override
    public String getGraphsPageTitle() {
        return "Statistics for node " + getNodeName();
    }

    @Override
    public Graph getGraph(int which, Date timePeriod, boolean hideManAborted) {
        // TODO Auto-generated method stub
        return null;
    }
}
