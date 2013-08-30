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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimePeriod;
import org.junit.Before;
import org.junit.Test;

import com.sonyericsson.jenkins.plugins.bfa.graphs.FailureCauseTimeInterval;
import com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

/**
 * Test class for statistics fetching using Embedded MongoDB.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class EmbeddedMongoStatisticsTest extends EmbeddedMongoTest {
    // CS IGNORE MagicNumber FOR NEXT 400 LINES. REASON: Test data.

    private static final String ID1 = "111111111111111111111111";
    private static final String ID2 = "222222222222222222222222";
    private static final String CAT1 = "CAT1";
    private static final String CAT2 = "CAT2";
    private static final String PROJECT_A = "Project A";
    private static final String PROJECT_B = "Project B";
    private static final String MASTER_A = "Master A";
    private static final String MASTER_B = "Master B";
    private static final String SUCCESS = "SUCCESS";
    private static final String ABORTED = "ABORTED";
    private TimePeriod hourPeriod1;
    private TimePeriod hourPeriod2;
    private GraphFilterBuilder filter1;
    private GraphFilterBuilder filter2;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        Calendar lastHour = Calendar.getInstance();
        lastHour.add(Calendar.HOUR_OF_DAY, -1);
        hourPeriod1 = new Hour(lastHour.getTime());
        hourPeriod2 = new Hour();
        filter1 = new GraphFilterBuilder();
        filter2 = new GraphFilterBuilder();
    }

    /**
     * Sets up the knowledgebase with two failure causes.
     * @throws Exception if something goes wrong
     */
    private void setUpTwoCauses() throws Exception {
        FailureCauseStatistics statCause1 = new FailureCauseStatistics(ID1, null);
        FailureCauseStatistics statCause2 = new FailureCauseStatistics(ID2, null);

        FailureCause cause1 = new FailureCause(ID1, null, null, CAT1, null);
        FailureCause cause2 = new FailureCause(ID2, null, null, CAT2, null);

        knowledgeBase.addCause(cause1);
        knowledgeBase.addCause(cause2);

        List<FailureCauseStatistics> failureList1 = new ArrayList<FailureCauseStatistics>();
        failureList1.add(statCause1);
        failureList1.add(statCause2);

        List<FailureCauseStatistics> failureList2 = new ArrayList<FailureCauseStatistics>();
        failureList2.add(statCause1);

        Calendar lastHour = Calendar.getInstance();
        lastHour.add(Calendar.HOUR_OF_DAY, -1);

        Statistics statistics1 = new Statistics(PROJECT_A, 1, lastHour.getTime(), 1L, null, null, MASTER_A, 0, SUCCESS,
                failureList1);
        Statistics statistics2 = new Statistics(PROJECT_B, 2, new Date(), 1L, null, null, MASTER_B, 0, ABORTED,
                failureList2);

        knowledgeBase.saveStatistics(statistics1);
        knowledgeBase.saveStatistics(statistics2);
    }

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

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getCount());
        assertEquals(1, result.get(1).getCount());
        assertEquals(ID1, result.get(0).getObject());
        assertEquals(ID2, result.get(1).getObject());
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

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getCount());
        assertEquals(1, result.get(1).getCount());
        assertEquals(ID1, result.get(0).getObject().getId());
        assertEquals(ID2, result.get(1).getObject().getId());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getFailureCausesPerBuild(GraphFilterBuilder)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetFailureCausesPerBuild() throws Exception {
        setUpTwoCauses();

        Map<Integer, List<FailureCause>> result = knowledgeBase.getFailureCausesPerBuild(null);

        // We have two build numbers:
        assertEquals(2, result.entrySet().size());
        // We have two FailureCauses for build #1:
        assertEquals(2, result.get(1).size());
        // We have one FailureCause for build #2:
        assertEquals(1, result.get(2).size());


        // For build #1, we have both failure causes:
        List<FailureCause> firstBuildFailureCauses = result.get(1);
        assertThat(firstBuildFailureCauses.get(0).getId(), anyOf(equalTo(ID1), equalTo(ID2)));
        assertThat(firstBuildFailureCauses.get(1).getId(), anyOf(equalTo(ID1), equalTo(ID2)));

        assertFalse("The ids should not be equal",
                firstBuildFailureCauses.get(0).equals(firstBuildFailureCauses.get(1)));

        // For build #2, we have only failure cause with id1:
        assertEquals(ID1, result.get(2).get(0).getId());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getFailureCausesPerTime(int, GraphFilterBuilder, boolean)}.
     * Fetches failure causes per time in hour intervals and does not group by categories.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetFailureCausesPerTimeNonCategories() throws Exception {
        setUpTwoCauses();

        List<FailureCauseTimeInterval> result = knowledgeBase.getFailureCausesPerTime(Calendar.HOUR_OF_DAY, null, false);

        // We expect 3 FailureCauseIntervals:
        assertEquals(3, result.size());

        assertEquals(ID1, result.get(0).getId());
        assertEquals(hourPeriod2, result.get(0).getPeriod());
        assertEquals(1, result.get(0).getNumber());

        assertEquals(ID2, result.get(1).getId());
        assertEquals(hourPeriod1, result.get(1).getPeriod());
        assertEquals(1, result.get(1).getNumber());

        assertEquals(ID1, result.get(2).getId());
        assertEquals(hourPeriod1, result.get(2).getPeriod());
        assertEquals(1, result.get(2).getNumber());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getFailureCausesPerTime(int, GraphFilterBuilder, boolean)}.
     * Fetches failure causes per time in hour intervals and groups by categories.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetFailureCausesPerTimeWithCategories() throws Exception {
        setUpTwoCauses();

        List<FailureCauseTimeInterval> result = knowledgeBase.getFailureCausesPerTime(Calendar.HOUR_OF_DAY, null, true);

        // We expect 3 FailureCauseIntervals:
        assertEquals(3, result.size());

        assertEquals(CAT1, result.get(0).getName());
        assertEquals(hourPeriod2, result.get(0).getPeriod());
        assertEquals(1, result.get(0).getNumber());

        assertEquals(CAT2, result.get(1).getName());
        assertEquals(hourPeriod1, result.get(1).getPeriod());
        assertEquals(1, result.get(1).getNumber());

        assertEquals(CAT1, result.get(2).getName());
        assertEquals(hourPeriod1, result.get(2).getPeriod());
        assertEquals(1, result.get(2).getNumber());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getFailureCausesPerTime(int, GraphFilterBuilder, boolean)}.
     * Fetches failure causes per time in day intervals and does not group by categories.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetFailureCausesPerTimeDayPeriod() throws Exception {
        setUpTwoCauses();

        List<FailureCauseTimeInterval> result = knowledgeBase.getFailureCausesPerTime(Calendar.DATE, null, false);

        // We expect 2 FailureCauseIntervals:
        assertEquals(2, result.size());

        assertEquals(ID2, result.get(0).getId());
        assertEquals(new Day(), result.get(0).getPeriod());
        assertEquals(1, result.get(0).getNumber());

        assertEquals(ID1, result.get(1).getId());
        assertEquals(new Day(), result.get(1).getPeriod());
        assertEquals(2, result.get(1).getNumber());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getUnknownFailureCauseQuotaPerTime(int, GraphFilterBuilder)}.
     * Sets up statistics for one day where 50% are without failure causes.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetUnknownFailureCauseQuotaPerTimeOneDay() throws Exception {
        FailureCause cause = new FailureCause(ID1, null, null, CAT1, null);
        knowledgeBase.saveCause(cause);

        FailureCauseStatistics causeStats = new FailureCauseStatistics(ID1, null);
        List<FailureCauseStatistics> statList = new ArrayList<FailureCauseStatistics>();
        statList.add(causeStats);

        Statistics statistics1 = new Statistics(null, 1, new Date(), 1L, null, null, null, 0, null, statList);
        Statistics statistics2 = new Statistics(null, 2, new Date(), 1L, null, null, null, 0, null, null);

        knowledgeBase.saveStatistics(statistics1);
        knowledgeBase.saveStatistics(statistics2);

        TimePeriod today = new Day();

        Map<TimePeriod, Double> result = knowledgeBase.getUnknownFailureCauseQuotaPerTime(Calendar.DATE, null);

        assertEquals(1, result.keySet().size());
        assertEquals(0, Double.compare(result.values().iterator().next(), 0.5));
        assertEquals(today, result.keySet().iterator().next());
    }

    /**
     * Test for filtering statistics by project.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnProject() throws Exception {
        setUpTwoCauses();

        filter1.setProjectName(PROJECT_A);
        filter2.setProjectName(PROJECT_B);

        doFilterAssert();
    }

    /**
     * Test for filtering statistics by build number.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnBuildNumber() throws Exception {
        setUpTwoCauses();

        filter1.setBuildNumbers(Arrays.asList(1));
        filter2.setBuildNumbers(Arrays.asList(2));

        doFilterAssert();
    }

    /**
     * Test for filtering statistics by master.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnMaster() throws Exception {
        setUpTwoCauses();

        filter1.setMasterName(MASTER_A);
        filter2.setMasterName(MASTER_B);

        doFilterAssert();
    }

    /**
     * Test for filtering statistics by build started.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnBuildStarted() throws Exception {
        setUpTwoCauses();

        Calendar fewMinutesAgo = Calendar.getInstance();
        fewMinutesAgo.add(Calendar.MINUTE, -5);
        Calendar moreThanAnHourAgo = Calendar.getInstance();
        moreThanAnHourAgo.add(Calendar.MINUTE, -65);

        filter1.setSince(moreThanAnHourAgo.getTime());
        filter2.setSince(fewMinutesAgo.getTime());

        doFilterAssert();
    }

    /**
     * Test for filtering statistics by result.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnResult() throws Exception {
        setUpTwoCauses();

        filter1.setResult(SUCCESS);
        filter2.setResult(ABORTED);

        doFilterAssert();
    }

    /**
     * Test for filtering statistics by excluded result.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFilterOnExcludeResult() throws Exception {
        setUpTwoCauses();

        filter1.setExcludeResult(ABORTED);
        filter2.setExcludeResult(SUCCESS);

        doFilterAssert();
    }

    /**
     * Helper for the filter tests. Verifies that the correct result is returned since
     * most filter tests have the same expected result.
     */
    private void doFilterAssert() {
        List<ObjectCountPair<FailureCause>> result1 = knowledgeBase.getNbrOfFailureCauses(filter1);
        List<ObjectCountPair<FailureCause>> result2 = knowledgeBase.getNbrOfFailureCauses(filter2);

        assertEquals(2, result1.size());
        String resultId1 = result1.get(0).getObject().getId();
        String resultId2 = result1.get(1).getObject().getId();
        assertThat(resultId1, anyOf(equalTo(ID1), equalTo(ID2)));
        assertThat(resultId2, anyOf(equalTo(ID1), equalTo(ID2)));
        assertFalse("Both ids should not be equal", resultId1.equals(resultId2));

        assertEquals(1, result2.size());
        assertEquals(ID1, result2.get(0).getObject().getId());
    }

}
