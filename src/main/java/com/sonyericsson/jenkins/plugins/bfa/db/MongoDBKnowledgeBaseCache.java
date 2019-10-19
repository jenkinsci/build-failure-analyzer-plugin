/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa.db;

import com.mongodb.MongoException;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase.NOT_REMOVED_QUERY;

/**
 * Cache for the MongoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class MongoDBKnowledgeBaseCache {

    private Semaphore shouldUpdate;
    private UpdateThread updaterThread;
    private Timer timer;
    private TimerTask timerTask;
    private List<FailureCause> cachedFailureCauses;
    private List<String> categories;
    private JacksonDBCollection<FailureCause, String> jacksonCollection;

    private static final long CACHE_UPDATE_INTERVAL = 60000;
    private static final Logger logger = Logger.getLogger(MongoDBKnowledgeBase.class.getName());

    /**
     * Standard constructor.
     * @param jacksonCollection the JacksonDBCollection, used for accessing the database.
     */
    public MongoDBKnowledgeBaseCache(JacksonDBCollection<FailureCause, String> jacksonCollection) {
        this.jacksonCollection = jacksonCollection;
    }

    /**
     * Run when the cache, including the update mechanism, should start running.
     */
    public void start() {
        shouldUpdate = new Semaphore();
        updaterThread = new UpdateThread();
        updaterThread.start();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                shouldUpdate.release();
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, CACHE_UPDATE_INTERVAL);
    }

    /**
     * Run when we want to shut down the cache.
     */
    public void stop() {
        timer.cancel();
        timer = null;
        timerTask = null;
        updaterThread.stopThread();
        updaterThread = null;
    }

    /**
     * Signal that an update of the Cache should be made.
     */
    public void updateCache() {
        if (shouldUpdate != null) {
            shouldUpdate.release();
        }
    }

    /**
     * Getter for the cachedFailureCauses.
     * @return the causes.
     */
    public List<FailureCause> getCauses() {
        return cachedFailureCauses;
    }

    /**
     * Getter for the categories of all FailureCauses.
     * @return the categories.
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * The thread responsible for updating the MongoDB cache.
     */
    protected class UpdateThread extends Thread {
        private volatile boolean stop = false;
            @Override
            public void run() {
                while (!stop) {
                    try {
                        shouldUpdate.acquire();
                        if (stop) {
                            break;
                        }
                        List<FailureCause> list = new LinkedList<FailureCause>();
                        DBCursor<FailureCause> dbCauses =  jacksonCollection.find(NOT_REMOVED_QUERY);
                        while (dbCauses.hasNext()) {
                            list.add(dbCauses.next());
                        }
                        cachedFailureCauses = list;
                        categories = jacksonCollection.distinct("categories");
                    } catch (MongoException e) {
                        logger.log(Level.SEVERE, "MongoException caught when updating cache: ", e);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Updater thread interrupted", e);
                    }
                }
            }
            /**
             * Stops the execution of this thread.
             */
            protected void stopThread() {
                stop = true;
                shouldUpdate.release();
            }
        }
    }
