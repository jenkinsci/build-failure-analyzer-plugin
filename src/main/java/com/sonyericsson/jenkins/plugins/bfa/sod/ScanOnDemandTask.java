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
package com.sonyericsson.jenkins.plugins.bfa.sod;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.BuildFailureScanner;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Run;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable class for scanning non scanned build.
 *
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandTask implements Runnable {

    private static final Logger logger = Logger.getLogger(ScanOnDemandTask.class.getName());
    private Run build;

    /**
     * SODExecutor constructor.
     *
     * @param build the build to analyze.
     */
    public ScanOnDemandTask(final Run build) {
        this.build = build;
    }

    @Override
    public void run() {
        try {
            if (build instanceof MatrixBuild) {
                List<MatrixRun> runs = ((MatrixBuild)build).getRuns();
                for (Run run : runs) {
                    if (run.getActions(FailureCauseBuildAction.class).isEmpty()
                            && run.getActions(FailureCauseMatrixBuildAction.class).isEmpty()
                            && PluginImpl.needToAnalyze(run.getResult())
                            && run.getNumber() == build.getNumber()) {
                        scanBuild(run);
                    }
                }
                endMatrixBuildScan();
            } else {
                scanBuild(build);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add a FailureScanner to "
                    + build.getParent().getFullDisplayName(), e);
        }
    }

    /**
     * Method will add matrix sub job
     * failure causes to parent job.
     * @throws IOException IOException
     */
    public void endMatrixBuildScan() throws IOException {
        List<MatrixRun> runs = ((MatrixBuild)build).getRuns();
        List<MatrixRun> runsWithCorrectNumber = new LinkedList<MatrixRun>();

        for (MatrixRun run : runs) {
            if (run.getNumber() == build.getNumber()) {
                runsWithCorrectNumber.add(run);
            }
        }
        build.addAction(new FailureCauseMatrixBuildAction((MatrixBuild)build, runsWithCorrectNumber));
        build.save();
    }

    /**
     * Scan the non scanned old build.
     *
     * @param run the non-scanned/scanned build to scan/rescan.
     */
    public void scanBuild(Run run) {
        try (
                FileOutputStream fos = new FileOutputStream(run.getLogFile(), true);
                PrintStream buildLog = new PrintStream(fos, true, "UTF8")
        ){
            PluginImpl.getInstance().getKnowledgeBase().removeBuildfailurecause(run);
            BuildFailureScanner.scanIfNotScanned(run, buildLog);
            run.save();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not get the causes from the knowledge base", e);
        }
    }
}
