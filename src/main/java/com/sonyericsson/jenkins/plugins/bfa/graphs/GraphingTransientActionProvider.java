package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.Arrays;
import java.util.Collection;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientComputerActionFactory;
import hudson.model.Computer;

/**
 * Extension point to insert graph statistics for nodes/computers.
 *
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 */
@Extension
public class GraphingTransientActionProvider extends TransientComputerActionFactory {

    @Override
    public Collection<? extends Action> createFor(Computer target) {
        final ComputerGraphAction compGraphAction = new ComputerGraphAction(target);

        return Arrays.asList(compGraphAction);
    }

}
