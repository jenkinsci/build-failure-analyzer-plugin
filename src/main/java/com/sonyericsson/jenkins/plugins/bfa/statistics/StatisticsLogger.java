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
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.LoggingWork.KbLoggingWork;
import com.sonyericsson.jenkins.plugins.bfa.statistics.LoggingWork.StatsdLoggingWork;
import com.timgroup.statsd.StatsDClient;
import hudson.model.Run;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Main singleton entrance for logging statistics.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public final class StatisticsLogger {

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
    public void log(Run build, List<FoundFailureCause> causes) {
        if (PluginImpl.getInstance().getKnowledgeBase().isStatisticsEnabled()) {
            queueExecutor.submit(new KbLoggingWork(build, causes));
        }

        if (PluginImpl.getInstance().isStatsdEnabled()) {
            StatsDClient statsClient = PluginImpl.getInstance().getStatsDClient();
            queueExecutor.submit(new StatsdLoggingWork(build, causes, statsClient));
        }
    }
}
