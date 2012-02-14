/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.util.LinkedList;
import java.util.List;

/**
 *  Aggregates the failure causes from downstream builds to the parent build.
 *  @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCauseMatrixAggregator extends MatrixAggregator {

    /**
     * Standard constructor.
     * @param build the MatrixBuild to aggregate FailureCauses for.
     * @param launcher the launcher.
     * @param listener the listener.
     */
    public FailureCauseMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        super(build, launcher, listener);
    }

    @Override
    public boolean endBuild() {
        if (build.getResult().isWorseThan(Result.SUCCESS)) {
            List<MatrixRun> runs = build.getRuns();
            List<MatrixRun> runsWithCorrectNumber = new LinkedList<MatrixRun>();
            for (MatrixRun run : runs) {
                if (run.getNumber() == build.getNumber()) {
                    runsWithCorrectNumber.add(run);
                }
            }
            build.addAction(new FailureCauseMatrixBuildAction(runsWithCorrectNumber));
        }
        return true;
    }
}
