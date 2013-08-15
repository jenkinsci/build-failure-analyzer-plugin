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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.util.Graph;
import hudson.util.RunList;

/**
 * Graph for the BFA plugin.
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public abstract class BFAGraph extends Graph {

    /**
     * The project that this graph is plotting values for. Is null for non-project graphs.
     */
    protected AbstractProject project;
    /**
     * The data filter used for this graph.
     */
    protected GraphFilterBuilder filter;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph, set to null for non-project graphs
     * @param filter the filter used when fetching data for this graph
     */
    protected BFAGraph(long timestamp, int defaultW, int defaultH, AbstractProject project, GraphFilterBuilder filter) {
        super(timestamp, defaultW, defaultH);
        this.project = project;
        this.filter = filter;
    }

    /**
     * Gets a list of build numbers that exist for the argument project.
     * @param project the project to list build numbers for
     * @return list of build numbers, empty if project is null
     */
    public static List<Integer> getBuildNumbers(AbstractProject project) {
        List<Integer> buildNumbers = new ArrayList<Integer>();
        if (project != null) {
            RunList runList = project.getBuilds();
            if (runList != null) {
                Iterator<Run> itr = runList.iterator();
                while (itr.hasNext()) {
                    Run currentRun = itr.next();
                    if (currentRun != null) {
                        buildNumbers.add(currentRun.getNumber());
                    }
                }
            }
        }
        return buildNumbers;
    }

}
