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

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread-pool and queue implementation for queueing builds for scanning.
 *
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public final class ScanOnDemandQueue {

    private static final Logger logger = LoggerFactory.getLogger(ScanOnDemandQueue.class);
    private static ScanOnDemandQueue instance;
    private ThreadPoolExecutor executor = null;
    /**
     * Private Default constructor.
     */
    private ScanOnDemandQueue() {
    }

    /**
     * Returns the singleton instance of the sod-queue.
     *
     * @return the instance.
     */
    public static synchronized ScanOnDemandQueue getInstance() {
        if (instance == null) {
            instance = new ScanOnDemandQueue();
            instance.startQueue();
        }
        return instance;
    }

    /**
     * Returns the current queue size.
     *
     * @return the queue size,
     */
    public static int getQueueSize() {
        if (instance != null && instance.executor != null) {
            return instance.executor.getQueue().size();
        } else {
            return 0;
        }
    }

    /**
     * Adds a sod-task to the singleton instance's queue.
     *
     * @param task the task to do.
     */
    public static void queue(ScanOnDemandTask task) {
        getInstance().queueTask(task);
    }

    /**
     * Starts the executor if it hasn't started yet, or updates the thread-pool size if it is started.
     *
     */
    protected void startQueue() {
        if (executor == null) {
            logger.debug("Starting the sending thread pool.");
            executor = new ThreadPoolExecutor(
                    PluginImpl.getInstance().getSodVariables().getMinimumSodWorkerThreads(),
                    PluginImpl.getInstance().getSodVariables().getMinimumSodWorkerThreads(),
                    PluginImpl.getInstance().getSodVariables().getSodThreadKeepAliveTime(), TimeUnit.MINUTES,
                    new LinkedBlockingQueue<Runnable>());
            executor.allowCoreThreadTimeOut(true);
            executor.prestartCoreThread();
            logger.info("SendQueue started! Current pool size: {}", executor.getPoolSize());
        }
        executor.setMaximumPoolSize(PluginImpl.getInstance().getSodVariables().getMaximumSodWorkerThreads());
        executor.setCorePoolSize(PluginImpl.getInstance().getSodVariables().getSodCorePoolNumberOfThreads());
        logger.debug("SendQueue running. Current pool size: {}. Current Queue size: {}",
                executor.getPoolSize(), getQueueSize());
        logger.debug("Nr of active pool-threads: {}", executor.getActiveCount());
    }

    /**
     * Adds a task to the queue.
     *
     * @param task the task to do.
     * @see java.util.concurrent.ThreadPoolExecutor#submit(Runnable)
     */
    public void queueTask(ScanOnDemandTask task) {
        try {
            logger.debug("Queueing task {}", task);
            executor.submit(task);
        } catch (RejectedExecutionException e) {
            logger.error("Unable to submit/queue a sod-task! ", task, e);
        }
    }

    /**
     * Shuts down the executor(s).
     * Gracefully waits for {@link #WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT} seconds for all jobs to finish
     * before forcefully shutting them down.
     */
    public static void shutdown() {
        if (instance != null && instance.executor != null) {
            ThreadPoolExecutor pool = instance.executor;
            pool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(PluginImpl.getInstance().
                        getSodVariables().getSodWaitForJobShutdownTimeout(),
                        TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(PluginImpl.getInstance().
                            getSodVariables().getSodWaitForJobShutdownTimeout(),
                            TimeUnit.SECONDS)) {
                        logger.error("Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }
}
