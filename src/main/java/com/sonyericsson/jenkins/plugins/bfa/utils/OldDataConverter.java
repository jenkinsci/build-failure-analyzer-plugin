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

package com.sonyericsson.jenkins.plugins.bfa.utils;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.listeners.ItemListener;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts old {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication}s with line number info to
 * the new format using line matcher.
 *
 * @author Robert Sandell &lt;robert.sandell@sonymobile.com&gt;
 */
@Extension
public final class OldDataConverter extends ItemListener {

    /**
     * The size of the thread pool.
     *
     * @see Executors#newScheduledThreadPool(int)
     */
    public static final int POOL_SIZE = 10;

    //CS IGNORE LineLength FOR NEXT 5 LINES. REASON: Javadoc
    /**
     * The seconds to delay actual processing of the conversion.
     *
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    public static final int SCHEDULE_DELAY = 3;

    private static final Logger logger = Logger.getLogger(OldDataConverter.class.getName());
    private static OldDataConverter instance;


    private Set<AbstractBuild> performedBuilds;
    private Map<String, List<FailureCauseMatrixBuildAction>> actionsToConvert;
    private ScheduledThreadPoolExecutor executor;
    //Has the call from Jenkins arrived that all items are loaded?
    private boolean itemsLoaded = false;

    /**
     * Retrieves the singleton instance from {@link hudson.model.listeners.ItemListener#all()}. If it is not found there
     * an {@link IllegalStateException} will be thrown.
     *
     * @return the converter.
     */
    public static synchronized OldDataConverter getInstance() {
        if (instance == null) {
            instance = ItemListener.all().get(OldDataConverter.class);
            if (instance == null) {
                throw new IllegalStateException("ItemListeners has not been loaded yet!");
            }
        }
        return instance;
    }

    /**
     * Default Constructor. <strong>Should only be instantiated by Jenkins</strong>
     */
    public OldDataConverter() {
        performedBuilds = Collections.synchronizedSet(new HashSet<AbstractBuild>());
        actionsToConvert = Collections.synchronizedMap(new HashMap<String, List<FailureCauseMatrixBuildAction>>());
        executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(POOL_SIZE);
    }

    /**
     * Adds the provided build to the queue of builds to convert {@link FoundIndication}s in, unless the conversion for
     * that build is already in progress.
     *
     * @param build the build to convert.
     */
    public void convertFoundIndications(AbstractBuild build) {
        //Just a convenience first check, because of the delay in scheduling
        // we will still get the same build multiple times in the executor, but the run method takes care of that.
        if (!performedBuilds.contains(build)) {
            executor.schedule(new FoundIndicationWork(build, performedBuilds), SCHEDULE_DELAY, TimeUnit.SECONDS);
        }
    }

    /**
     * Convert {@link FailureCauseMatrixBuildAction}s to use {@link FailureCauseMatrixBuildAction#runIds} instead of run
     * instances during serialization.
     * Will schedule the conversion until all items in Jenkins has been loaded.
     *
     * @param action            the action to fix.
     * @param matrixProjectName the name of the matrix project.
     */
    public synchronized void convertMatrixBuildAction(String matrixProjectName, FailureCauseMatrixBuildAction action) {
        if (itemsLoaded) {
            executor.schedule(new MatrixBuildActionWork(matrixProjectName, action), SCHEDULE_DELAY, TimeUnit.SECONDS);
        } else {
            List<FailureCauseMatrixBuildAction> actions = actionsToConvert.get(matrixProjectName);
            if (actions == null) {
                actions = new LinkedList<FailureCauseMatrixBuildAction>();
                actionsToConvert.put(matrixProjectName, actions);
            }
            actions.add(action);
        }

    }

    @Override
    public synchronized void onLoaded() {
        //Release the hounds!!!
        itemsLoaded = true;
        for (String project : actionsToConvert.keySet()) {
            List<FailureCauseMatrixBuildAction> actions = actionsToConvert.get(project);
            logger.log(Level.FINE, "Scheduling conversion of {1} build actions for project {2}.",
                    new Object[]{actions.size(), project});
            for (FailureCauseMatrixBuildAction action : actions) {
                executor.schedule(new MatrixBuildActionWork(project, action), SCHEDULE_DELAY, TimeUnit.SECONDS);
            }
        }
        actionsToConvert.clear();
    }

    /**
     * Waits until there are no more running conversion tasks.
     * Since builds are lazy loaded the returning of this method does
     * not mean that all needed builds are converted, only those loaded
     * before or during this method was invoked.
     *
     * @throws InterruptedException if so while sleeping
     */
    public void waitForInitialCompletion() throws InterruptedException {
        boolean loaded = false;
        while (!loaded) {
            synchronized (this) {
                loaded = this.itemsLoaded;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        while (!executor.getQueue().isEmpty()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     * Work to convert {@link FailureCauseMatrixBuildAction}s to use {@link FailureCauseMatrixBuildAction#runIds}
     * instead of run instances during serialization.
     */
    public static class MatrixBuildActionWork implements Runnable {
        String project;
        FailureCauseMatrixBuildAction action;

        /**
         * Standard Constructor.
         *
         * @param action  the action to fix.
         * @param project the name of the matrix project
         */
        public MatrixBuildActionWork(String project, FailureCauseMatrixBuildAction action) {
            this.project = project;
            this.action = action;
        }

        @Override
        public void run() {
            logger.log(Level.FINE, "Calling conversion of {0}", project);
            action.convertOldData();
        }
    }

    /**
     * A work task that does the actual conversion in an executor thread.
     */
    public static class FoundIndicationWork implements Runnable {
        private AbstractBuild build;
        private Set<AbstractBuild> performedBuilds;

        /**
         * Standard Constructor.
         *
         * @param build           the build to convert.
         * @param performedBuilds the list of in-progress or already converted builds.
         */
        public FoundIndicationWork(AbstractBuild build, Set<AbstractBuild> performedBuilds) {
            this.build = build;
            this.performedBuilds = performedBuilds;
        }

        @Override
        public void run() {
            if (performedBuilds.add(build)) { //if add returns false it means the build was already present in the set.
                FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
                if (action != null) {
                    try {
                        List<String> log = build.getLog(Integer.MAX_VALUE);
                        for (FoundFailureCause cause : action.getFoundFailureCauses()) {
                            for (FoundIndication indication : cause.getIndications()) {
                                indication.convertFromLineNumber(log);
                            }
                        }
                        build.save();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Failed to convert FoundIndications in "
                                + build.getFullDisplayName(), e);
                    }
                }
            }
        }
    }
}
