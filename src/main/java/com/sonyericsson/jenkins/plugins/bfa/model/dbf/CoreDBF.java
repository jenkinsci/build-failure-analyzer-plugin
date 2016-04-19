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

package com.sonyericsson.jenkins.plugins.bfa.model.dbf;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Fingerprint;
import hudson.model.Job;
import hudson.model.Run;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Basic implementation to get downstream builds from Jenkins using core
 * functionality.
 *
 * @author Jan-Olof Sivtoft
 */
@Extension
public class CoreDBF extends DownstreamBuildFinder {

    /**
     * Return a list of all downstream builds originating from provided build.
     * Using core functionality to retrieve build(s).
     *
     * @param build get the downstream build(s) relative this build
     * @return alist with downstream builds
     */
    @Override
    public List<Run<?, ?>> getDownstreamBuilds(
            final Run build) {

        Map<Job, Fingerprint.RangeSet> buildMap = null;
        if (build instanceof AbstractBuild) {
            buildMap = ((AbstractBuild)build).getDownstreamBuilds();
        }
        LinkedList<Run<?, ?>> foundBuilds =
                new LinkedList<Run<?, ?>>();


        if (buildMap != null && !buildMap.isEmpty()) {
            for (Map.Entry<Job, Fingerprint.RangeSet> entry
                    : buildMap.entrySet()) {
                for (Integer buildId : entry.getValue().listNumbers()) {
                    foundBuilds.add((Run<?, ?>)
                            entry.getKey().getBuildByNumber(buildId));
                }
            }
        }

        return foundBuilds;
    }
}
