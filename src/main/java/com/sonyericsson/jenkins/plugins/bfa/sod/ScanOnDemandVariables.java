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

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Scan on demand feature settings.
 *
 * @author Shemeer S &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandVariables {

    /**
     * Minimum number of worker threads.
     */
    public static final int DEFAULT_MINIMUM_SOD_WORKER_THREADS = 1;
    /**
     * Maximum number of worker threads.
     */
    public static final int DEFAULT_MAXIMUM_SOD_WORKER_THREADS = 1;
    /**
     * Thread keep alive time.
     */
    public static final int DEFAULT_SOD_THREADS_KEEP_ALIVE_TIME = 15;
    /**
     * Wait for job shutdown time.
     */
    public static final int DEFAULT_SOD_WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT = 30;
    /**
     * Number of Core pool Threads.
     */
    public static final int DEFAULT_SOD_COREPOOL_THREADS = 5;

    /**
     * ScanOnDemand feature thread values.
     */
    private int minimumSodWorkerThreads;
    private int maximumSodWorkerThreads;
    private int sodThreadKeepAliveTime;
    private int sodWaitForJobShutdownTimeout;
    private int sodCorePoolNumberOfThreads;

    /**
     * Data bound constructor (used by jcasc).
     */
    @DataBoundConstructor
    public ScanOnDemandVariables() {
    }

    /**
     * Set maximum numbers of sod worker threads.
     *
     * @param maximumSodWorkerThreads value.
     */
     @DataBoundSetter
     public void setMaximumSodWorkerThreads(int maximumSodWorkerThreads) {
         this.maximumSodWorkerThreads = maximumSodWorkerThreads;
     }

    /**
     * Set minimum numbers of sod worker threads.
     *
     * @param minimumSodWorkerThreads value.
     */
    @DataBoundSetter
    public void setMinimumSodWorkerThreads(int minimumSodWorkerThreads) {
        this.minimumSodWorkerThreads = minimumSodWorkerThreads;
    }
    /**
     * Set number of sod corepool threads.
     *
     * @param sodCorePoolNumberOfThreads value.
     */
    @DataBoundSetter
    public void setSodCorePoolNumberOfThreads(int sodCorePoolNumberOfThreads) {
        this.sodCorePoolNumberOfThreads = sodCorePoolNumberOfThreads;
    }
    /**
     * Set sod threadkeepalivetime.
     *
     * @param sodThreadKeepAliveTime value.
     */
    @DataBoundSetter
    public void setSodThreadKeepAliveTime(int sodThreadKeepAliveTime) {
        this.sodThreadKeepAliveTime = sodThreadKeepAliveTime;
    }
    /**
     * Set sod wait for job shut down time.
     *
     * @param sodWaitForJobShutdownTimeout  value.
     */
    @DataBoundSetter
    public void setSodWaitForJobShutdownTimeout(int sodWaitForJobShutdownTimeout) {
        this.sodWaitForJobShutdownTimeout = sodWaitForJobShutdownTimeout;
    }

     /**
     * Returns the corepool number of threads.
     *
     * @return int value.
     */
     public int getSodCorePoolNumberOfThreads() {
         if (sodCorePoolNumberOfThreads < ScanOnDemandVariables.DEFAULT_SOD_COREPOOL_THREADS) {
             return ScanOnDemandVariables.DEFAULT_SOD_COREPOOL_THREADS;
         }

         return sodCorePoolNumberOfThreads;
     }

    /**
     * Returns the maximum number of sod threads.
     *
     * @return int value.
     */
    public int getMaximumSodWorkerThreads() {
        if (maximumSodWorkerThreads < ScanOnDemandVariables.DEFAULT_MAXIMUM_SOD_WORKER_THREADS) {
            return ScanOnDemandVariables.DEFAULT_MAXIMUM_SOD_WORKER_THREADS;
        }

        return maximumSodWorkerThreads;
    }

    /**
     * Returns the minimum number of sod threads.
     *
     * @return int value.
     */
    public int getMinimumSodWorkerThreads() {
        if (minimumSodWorkerThreads < ScanOnDemandVariables.DEFAULT_MINIMUM_SOD_WORKER_THREADS) {
            return ScanOnDemandVariables.DEFAULT_MINIMUM_SOD_WORKER_THREADS;
        }

        return minimumSodWorkerThreads;
    }

    /**
     * Returns the sod thread keep alive time.
     *
     * @return int value.
     */
    public int getSodThreadKeepAliveTime() {
        if (sodThreadKeepAliveTime < ScanOnDemandVariables.DEFAULT_SOD_THREADS_KEEP_ALIVE_TIME) {
            return ScanOnDemandVariables.DEFAULT_SOD_THREADS_KEEP_ALIVE_TIME;
        }

        return sodThreadKeepAliveTime;
    }

    /**
     * Returns the wait for job shut down time.
     *
     * @return the int value.
     */
    public int getSodWaitForJobShutdownTimeout() {
        if (sodWaitForJobShutdownTimeout < ScanOnDemandVariables.DEFAULT_SOD_WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT) {
            return DEFAULT_SOD_WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT;
        }

        return sodWaitForJobShutdownTimeout;
    }

}
