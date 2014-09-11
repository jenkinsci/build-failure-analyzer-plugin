/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications Inc. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa.statistics;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Node;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main singleton entrance for logging statistics.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public final class StatisticsLogger {

    private static final Logger logger = Logger.getLogger(StatisticsLogger.class.getName());
    private static StatisticsLogger instance;
    private ExecutorService queueExecutor;


    /**
     * Private Constructor.
     *
     * @see #getInstance()
     */
    private StatisticsLogger() {
        queueExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable target) {
                return new Thread(target, "BFA StatisticsLogger Queue");
            }
        });
    }

    /**
     * The singleton instance.
     *
     * @return the instance.
     */
    public static synchronized StatisticsLogger getInstance() {
        if (instance == null) {
            instance = new StatisticsLogger();
        }
        return instance;
    }

    /**
     * Logs a found indication asynchronously to the statistics database.
     *
     * @param build the build.
     * @param causes the list of causes.
     */
    public void log(AbstractBuild build, List<FoundFailureCause> causes) {
        if (PluginImpl.getInstance().getKnowledgeBase().isStatisticsEnabled()) {
            queueExecutor.submit(new LoggingWork(build, causes));
        }
    }

    /**
     * The actual work to be performed in {@link #log} at a future time.
     */
    static class LoggingWork implements Runnable {

        List<FoundFailureCause> causes;
        AbstractBuild build;

        /**
         * Standard Constructor.
         *
         * @param build the build to log for.
         * @param causes the causes to log.
         */
        LoggingWork(AbstractBuild build, List<FoundFailureCause> causes) {
            this.build = build;
            this.causes = causes;
        }

        @Override
        public void run() {
            String projectName = build.getProject().getFullName();
            int buildNumber = build.getNumber();
            Date startingTime = build.getTime();
            long duration = build.getDuration();
            List<String> triggerCauses = new LinkedList<String>();
            for (Object o : build.getCauses()) {
                triggerCauses.add(o.getClass().getSimpleName());
            }
            Node node = build.getBuiltOn();
            String nodeName = node.getNodeName();
            int timeZoneOffset = TimeZone.getDefault().getRawOffset();
            String master = "";

            String result = build.getResult().toString();
            List<FailureCauseStatistics> failureCauseStatistics = new LinkedList<FailureCauseStatistics>();
            List<String> causeIds = new LinkedList<String>();
            for (FoundFailureCause cause : causes) {
                FailureCauseStatistics stats = new FailureCauseStatistics(cause.getId(), cause.getIndications());
                failureCauseStatistics.add(stats);
                causeIds.add(cause.getId());
            }

            master = BfaUtils.getMasterName();
            Cause.UpstreamCause uc = (Cause.UpstreamCause)build.getCause(Cause.UpstreamCause.class);
            Statistics.UpstreamCause suc = new Statistics.UpstreamCause(uc);
            Statistics obj = new Statistics(projectName, buildNumber, startingTime, duration, triggerCauses, nodeName,
                                            master, timeZoneOffset, result, suc, failureCauseStatistics);

            PluginImpl p = PluginImpl.getInstance();
            KnowledgeBase kb = p.getKnowledgeBase();
            try {
                kb.saveStatistics(obj);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't save statistics: ", e);
            }

            kb.updateLastSeen(causeIds, startingTime);
        }
    }
}
