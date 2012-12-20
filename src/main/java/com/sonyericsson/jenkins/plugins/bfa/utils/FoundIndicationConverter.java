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
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts old {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication}s with line number info to
 * the new format using line matcher.
 *
 * @author Robert Sandell &lt;robert.sandell@sonymobile.com&gt;
 */
public final class FoundIndicationConverter {

    /**
     * The size of the thread pool.
     *
     * @see Executors#newScheduledThreadPool(int)
     */
    public static final int POOL_SIZE = 10;

    /**
     * The seconds to delay actual processing of the conversion.
     *
     * @see ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    public static final int SCHEDULE_DELAY = 3;

    private static final Logger logger = Logger.getLogger(FoundIndicationConverter.class.getName());
    private static FoundIndicationConverter instance;


    private Set<AbstractBuild> performedBuilds;
    private ScheduledExecutorService executor;

    /**
     * Retrieves the singleton instance, creates one if it is the first to execute.
     *
     * @return the converter.
     */
    public static synchronized FoundIndicationConverter getInstance() {
        if (instance == null) {
            instance = new FoundIndicationConverter();
        }
        return instance;
    }

    /**
     * Default utility Constructor.
     */
    private FoundIndicationConverter() {
        performedBuilds = Collections.synchronizedSet(new HashSet<AbstractBuild>());
        executor = Executors.newScheduledThreadPool(POOL_SIZE);
    }

    /**
     * Adds the provided build to the queue of builds to convert, unless the conversion for that build is already in
     * progress.
     *
     * @param build the build to convert.
     */
    public void convert(AbstractBuild build) {
        //Just a convenience first check, because of the delay in scheduling
        // we will still get the same build multiple times in the executor, but the run method takes care of that.
        if (!performedBuilds.contains(build)) {
            executor.schedule(new Work(build, performedBuilds), SCHEDULE_DELAY, TimeUnit.SECONDS);
        }
    }

    /**
     * A work task that does the actual conversion in an executor thread.
     */
    public static class Work implements Runnable {
        private AbstractBuild build;
        private Set<AbstractBuild> performedBuilds;

        /**
         * Standard Constructor.
         *
         * @param build           the build to convert.
         * @param performedBuilds the list of in-progress or already converted builds.
         */
        public Work(AbstractBuild build, Set<AbstractBuild> performedBuilds) {
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
