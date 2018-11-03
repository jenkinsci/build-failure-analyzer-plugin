/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications Inc. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.graphs.FailureCauseTimeInterval;
import com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseModification;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics.UpstreamCause;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimePeriod;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for statistics fetching using Embedded MongoDB.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PluginImpl.class)
@PowerMockIgnore("javax.management.*") //Solves PowerMock issue 277
public class EmbeddedMongoStatisticsTest extends EmbeddedMongoTest {
    // CS IGNORE MagicNumber FOR NEXT 600 LINES. REASON: Test data.

    private static final String ID1 = "111111111111111111111111";
    private static final String ID2 = "222222222222222222222222";
    private static final String CAT1 = "CAT1";
    private static final String CAT2 = "CAT2";
    private static final String PROJECT_A = "Project A";
    private static final String PROJECT_B = "Project B";
    private static final String PROJECT_C = "Project C";
    private static final String MASTER_A = "Master A";
    private static final String MASTER_B = "Master B";
    private static final String MASTER_C = "Master C";
    private static final String SUCCESS = "SUCCESS";
    private static final String ABORTED = "ABORTED";
    private static final String UNSTABLE = "UNSTABLE";
    private static final int BUILD_NR = 54321;
    private static Date lastHour;
    private static Date now;
    private TimePeriod hourPeriod1;
    private TimePeriod hourPeriod2;
    private GraphFilterBuilder filter1;
    private GraphFilterBuilder filter2;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        Calendar lastHourCalendar = Calendar.getInstance();
        lastHourCalendar.add(Calendar.HOUR_OF_DAY, -1);
        lastHour = lastHourCalendar.getTime();
        now = new Date();
        hourPeriod1 = new Hour(lastHour);
        hourPeriod2 = new Hour();
        filter1 = new GraphFilterBuilder();
        filter2 = new GraphFilterBuilder();

        PluginImpl plugin = new PluginImpl();
        mockStatic(PluginImpl.class);
        when(PluginImpl.getInstance()).thenReturn(plugin);
        Whitebox.setInternalState(plugin, "knowledgeBase", knowledgeBase);
    }

    /**
     * Sets up the knowledgebase with two failure causes.
     * @throws Exception if something goes wrong
     */
    private void setUpTwoCauses() throws Exception {
        FailureCauseStatistics statCause1 = new FailureCauseStatistics(ID1, null);
        FailureCauseStatistics statCause2 = new FailureCauseStatistics(ID2, null);

        FailureCause cause1 = new FailureCause(ID1, null, null, null, null, CAT1, null, null);
        FailureCause cause2 = new FailureCause(ID2, null, null, null, null, CAT2, null, null);

        knowledgeBase.addCause(cause1);
        knowledgeBase.addCause(cause2);

        List<FailureCauseStatistics> failureList1 = new ArrayList<FailureCauseStatistics>();
        failureList1.add(statCause1);
        failureList1.add(statCause2);

        List<FailureCauseStatistics> failureList2 = new ArrayList<FailureCauseStatistics>();
        failureList2.add(statCause1);

        List<FailureCauseStatistics> failureList3 = new ArrayList<FailureCauseStatistics>();

        Statistics statistics1 = new Statistics(PROJECT_A, 1, "", lastHour, 1L, null, null, MASTER_A, 0, UNSTABLE,
                                                null, failureList1);
        Statistics statistics2 = new Statistics(PROJECT_B, 2, "", now, 1L, null, null, MASTER_B, 0, ABORTED,
                                                null, failureList2);
        Statistics statistics3 = new Statistics(PROJECT_C, 3, "", now, 1L, null, null, MASTER_C, 0, SUCCESS,
                                                null, failureList3);

        knowledgeBase.saveStatistics(statistics1);
        knowledgeBase.saveStatistics(statistics2);
        knowledgeBase.saveStatistics(statistics3);
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getLatestFailureForCause(String)}.
     * @throws Exception if something goes wrong
     */
    @Test
    @Ignore("Fails on new version of the mongo java driver - returns null instead of expected date")
    public void testGetLatestFailureForCause() throws Exception {
        setUpTwoCauses();

        assertEquals(EmbeddedMongoStatisticsTest.now, knowledgeBase.getLatestFailureForCause(ID1));
        assertEquals(lastHour, knowledgeBase.getLatestFailureForCause(ID2));
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#updateLastSeen(List, Date)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testUpdateLastSeen() throws Exception {
        setUpTwoCauses();
        List<String> ids = Arrays.asList(ID1, ID2);
        knowledgeBase.updateLastSeen(ids, now);

        assertEquals(now, knowledgeBase.getCause(ID1).getLastOccurred());
        assertEquals(now, knowledgeBase.getCause(ID2).getLastOccurred());
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getCreationDateForCause(String)} by saving
     * a dummy {@link FailureCause} and verifying it was created recently.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetCreationDateForCause() throws Exception {
        FailureCause emptyCause = new FailureCause(null, null, null, null, null, "", null, null);
        String id = knowledgeBase.addCause(emptyCause).getId();

        Date creationDate = knowledgeBase.getCreationDateForCause(id);
        long createdMillisAgo = System.currentTimeMillis() - creationDate.getTime();
        int createdSecondsAgo = (int)TimeUnit.SECONDS.convert(createdMillisAgo, TimeUnit.MILLISECONDS);

        assertThat(createdSecondsAgo, is(lessThan(2)));
    }

    /**
     * Tests {@link MongoDBKnowledgeBase#getNbrOfNullFailureCauses
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder)}.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetNbrOfNullFailureCauses() throws Exception {
        assertEquals(0, knowledgeBase.getNbrOfNullFailureCauses(null));

        Statistics nullStatistics = new Statistics(null, 1, null, null, 1L, null, null, null, 1, null, null, null);
        knowledgeBase.saveStatistics(nullStatistics);

        assertEquals(1, knowledgeBase.getNbrOfNullFailureCauses(null));
    }

    /**
     * Test for {@link FailureCause#initModifications()}. Verifies that the creation date
     * is taken as latest modification date if no saved modifications are present.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testInitModifications() throws Exception {
        FailureCause cause = knowledgeBase.addCause(new FailureCause());

        List<FailureCauseModification> mods = cause.getAndInitiateModifications();

        assertFalse("Should have creation date after initiation", mods.isEmpty());
        long createdMillisAgo = System.currentTimeMillis() - mods.get(0).getTime().getTime();
        int createdSecondsAgo = (int)TimeUnit.SECONDS.convert(createdMillisAgo, TimeUnit.MILLISECONDS);

        assertThat(createdSecondsAgo, is(lessThan(2)));
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
     * Tests that {@link MongoDBKnowledgeBase#getStatistics
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder)} with upstream cause.gets stored correctly.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testStatisticsWithUpstreamCauses() throws Exception {
        Statistics.UpstreamCause uc = new UpstreamCause(PROJECT_B, BUILD_NR);
        Statistics s = new Statistics(PROJECT_A, 1, null, null, 1L, null, null, MASTER_A, 0, UNSTABLE, uc, null);
        knowledgeBase.saveStatistics(s);
        List<Statistics> fetchedStatistics = knowledgeBase.getStatistics(null, -1);
        assertNotNull("The fetched statistics should not be null", fetchedStatistics);
        assertFalse("The fetched statistics list should not be empty", fetchedStatistics.isEmpty());
        Statistics.UpstreamCause fetchedUC = fetchedStatistics.get(0).getUpstreamCause();
        assertEquals(fetchedUC.getUpstreamBuild(), uc.getUpstreamBuild());
        assertEquals(fetchedUC.getUpstreamProject(), uc.getUpstreamProject());
    }

    /**
     * Tests that {@link MongoDBKnowledgeBase#getStatistics
     * (com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder)} with no upstream cause works correctly.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testStatisticsWithNoUpstreamCauses() throws Exception {
        Statistics s = new Statistics(PROJECT_A, 1, null, null, 1L, null, null, MASTER_A, 0, UNSTABLE, null, null);
        knowledgeBase.saveStatistics(s);
        List<Statistics> fetchedStatistics = knowledgeBase.getStatistics(null, -1);
        assertNotNull("The fetched statistics should not be null", fetchedStatistics);
        assertFalse("The fetched statistics list should not be empty", fetchedStatistics.isEmpty());
        Statistics.UpstreamCause fetchedUC = fetchedStatistics.get(0).getUpstreamCause();
        assertTrue("The fetched upstream cause should be null", fetchedUC == null);
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

        boolean firstFound = false;
        boolean secondFound = false;
        boolean thirdFound = false;

        for (FailureCauseTimeInterval timeInterval : result) {
            if (timeInterval.getId().equals(ID1)
                    && timeInterval.getPeriod().equals(hourPeriod2)
                    && timeInterval.getNumber() == 1) {
                firstFound = true;
            } else if (timeInterval.getId().equals(ID2)
                    && timeInterval.getPeriod().equals(hourPeriod1)
                    && timeInterval.getNumber() == 1) {
                secondFound = true;
            } else if (timeInterval.getId().equals(ID1)
                    && timeInterval.getPeriod().equals(hourPeriod1)
                    && timeInterval.getNumber() == 1) {
                thirdFound = true;
            }
        }

        assertTrue(firstFound);
        assertTrue(secondFound);
        assertTrue(thirdFound);
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

        boolean firstFound = false;
        boolean secondFound = false;
        boolean thirdFound = false;

        for (FailureCauseTimeInterval timeInterval : result) {
            if (timeInterval.getName().equals(CAT1)
                    && timeInterval.getPeriod().equals(hourPeriod2)
                    && timeInterval.getNumber() == 1) {
                firstFound = true;
            } else if (timeInterval.getName().equals(CAT2)
                    && timeInterval.getPeriod().equals(hourPeriod1)
                    && timeInterval.getNumber() == 1) {
                secondFound = true;
            } else if (timeInterval.getName().equals(CAT1)
                    && timeInterval.getPeriod().equals(hourPeriod1)
                    && timeInterval.getNumber() == 1) {
                thirdFound = true;
            }
        }

        assertTrue(firstFound);
        assertTrue(secondFound);
        assertTrue(thirdFound);
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
        FailureCause cause = new FailureCause(ID1, null, null, null, null, CAT1, null, null);
        knowledgeBase.saveCause(cause);

        FailureCauseStatistics causeStats = new FailureCauseStatistics(ID1, null);
        List<FailureCauseStatistics> statList = new ArrayList<FailureCauseStatistics>();
        statList.add(causeStats);

        Statistics statistics1 = new Statistics(null, 1, null, new Date(), 1L, null, null, null, 0, null, null,
                                                statList);
        Statistics statistics2 = new Statistics(null, 2, null, new Date(), 1L, null, null, null, 0, null, null, null);

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

        doFilterAssert(2, 2);
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

        doFilterAssert(2, 2);
    }

    /**
     * Shortcut for the filter assert helper with default parameters.
     */
    private void doFilterAssert() {
        doFilterAssert(2, 1);
    }

    /**
     * Helper for the filter tests. Verifies that the correct result is returned since
     * most filter tests have the same expected result.
     *
     * @param firstSize the expected result size from {@link #filter1}.
     * @param secondSize the expected result size from {@link #filter2}.
     */
    private void doFilterAssert(int firstSize, int secondSize) {
        List<ObjectCountPair<FailureCause>> result1 = knowledgeBase.getNbrOfFailureCauses(filter1);
        List<ObjectCountPair<FailureCause>> result2 = knowledgeBase.getNbrOfFailureCauses(filter2);

        assertEquals(firstSize, result1.size());
        String resultId1 = result1.get(0).getObject().getId();
        String resultId2 = result1.get(1).getObject().getId();
        assertThat(resultId1, anyOf(equalTo(ID1), equalTo(ID2)));
        assertThat(resultId2, anyOf(equalTo(ID1), equalTo(ID2)));
        assertFalse("Both ids should not be equal", resultId1.equals(resultId2));

        assertEquals(secondSize, result2.size());
        assertEquals(ID1, result2.get(0).getObject().getId());
    }

}
