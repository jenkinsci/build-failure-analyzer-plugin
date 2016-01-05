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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;


/**
 * Get downstream builds for Build Flows.
 *
 * @author Stephan Pauxberger
 */
@Extension
public class BuildFlowDBF extends DownstreamBuildFinder {

    private static final Logger logger = Logger.
            getLogger(BuildFlowDBF.class.getName());
    @Override
    public List<AbstractBuild<?, ?>> getDownstreamBuilds(
            final AbstractBuild build) {

        if (build == null) {
            return EMPTY;
        }

        if (!build.getClass().getName().equals("com.cloudbees.plugins.flow.FlowRun")) {
            return EMPTY;
        }

        Set<com.cloudbees.plugins.flow.JobInvocation> vertexSet =
                ((com.cloudbees.plugins.flow.FlowRun)build).getJobsGraph().vertexSet();

        List<AbstractBuild<?, ?>> result = new ArrayList<AbstractBuild<?, ?>>(vertexSet.size());

        //CS IGNORE EmptyBlock FOR NEXT 10 LINES. REASON: irrelevant exceptions.
        for (com.cloudbees.plugins.flow.JobInvocation invocation : vertexSet) {
            try {
                result.add((AbstractBuild<?, ?>)invocation.getBuild());
            } catch (ExecutionException e) {
                // skip
            } catch (InterruptedException e) {
                // ignore
            } catch (CancellationException e) {
                // ignore
            }
        }

        result = result.subList(1, result.size());

        return result;
    }
}
