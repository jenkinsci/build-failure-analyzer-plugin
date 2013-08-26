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
package com.sonyericsson.jenkins.plugins.bfa.db;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

import static org.junit.Assert.assertEquals;

/**
 * Test class for statistics fetching using Embedded MongoDB.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class EmbeddedMongoStatisticsTest extends EmbeddedMongoTest {

    private final String id1 = "111111111111111111111111";
    private final String id2 = "222222222222222222222222";

    /**
     * Tests {@link MongoDBKnowledgeBase#getNbrOfNullFailureCauses
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetNbrOfNullFailureCauses() throws Exception {
        assertEquals(0, knowledgeBase.getNbrOfNullFailureCauses(null));

        Statistics nullStatistics = new Statistics(null, 1, null, 1L, null, null, null, 1, null, null);
        knowledgeBase.saveStatistics(nullStatistics);

        assertEquals(1, knowledgeBase.getNbrOfNullFailureCauses(null));
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getNbrOfFailureCausesPerId
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder, int)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetNbrOfFailureCausesPerId() throws Exception {
        setUpTwoCauses();

        List<ObjectCountPair<String>> result = knowledgeBase.getNbrOfFailureCausesPerId(null, 0);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(2, result.get(0).getCount());
        Assert.assertEquals(1, result.get(1).getCount());
        Assert.assertEquals(id1, result.get(0).getObject());
        Assert.assertEquals(id2, result.get(1).getObject());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getNbrOfFailureCauses
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetNbrOfFailureCauses() throws Exception {
        setUpTwoCauses();

        List<ObjectCountPair<FailureCause>> result = knowledgeBase.getNbrOfFailureCauses(null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(2, result.get(0).getCount());
        Assert.assertEquals(1, result.get(1).getCount());
        Assert.assertEquals(id1, result.get(0).getObject().getId());
        Assert.assertEquals(id2, result.get(1).getObject().getId());
    }

    /**
     * Sets up the knowledgebase with two failure causes.
     * @throws Exception if something goes wrong
     */
    private void setUpTwoCauses() throws Exception {
        FailureCauseStatistics statCause1 = new FailureCauseStatistics(id1, null);
        FailureCauseStatistics statCause2 = new FailureCauseStatistics(id2, null);

        FailureCause cause1 = new FailureCause(id1, null, null, "", null);
        FailureCause cause2 = new FailureCause(id2, null, null, "", null);

        knowledgeBase.addCause(cause1);
        knowledgeBase.addCause(cause2);

        List<FailureCauseStatistics> failureList1 = new ArrayList<FailureCauseStatistics>();
        failureList1.add(statCause1);
        failureList1.add(statCause2);

        List<FailureCauseStatistics> failureList2 = new ArrayList<FailureCauseStatistics>();
        failureList2.add(statCause1);

        Statistics statistics1 = new Statistics(null, 1, null, 1L, null, null, null, 1, null, failureList1);
        Statistics statistics2 = new Statistics(null, 2, null, 1L, null, null, null, 1, null, failureList2);

        knowledgeBase.saveStatistics(statistics1);
        knowledgeBase.saveStatistics(statistics2);
    }

}
