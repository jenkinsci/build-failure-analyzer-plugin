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

package com.sonyericsson.jenkins.plugins.bfa.db;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongojack.JacksonMongoCollection;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


/**
 * Tests for the MongoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JacksonMongoCollection.class, Jenkins.class, Metrics.class, MetricRegistry.class})
public class MongoDBKnowledgeBaseTest {

    private JacksonMongoCollection<FailureCause> collection;
    private JacksonMongoCollection<DBObject> statisticsCollection;
    private MongoDBKnowledgeBase kb;
    private List<Indication> indications;
    private Indication indication;
    private FailureCause mockedCause;
    private static final int PORT = 27017;

    @Mock
    private Jenkins jenkins;
    @Mock
    private Metrics metricsPlugin;
    @Mock
    private MetricRegistry metricRegistry;

    /**
     * Common stuff to set up for the tests.
     */
    @Before
    public void setUp() {
        kb = new MongoDBKnowledgeBase("", PORT, "mydb", null, null, false, false);
        collection = mock(JacksonMongoCollection.class);
        statisticsCollection = mock(JacksonMongoCollection.class);
        Whitebox.setInternalState(kb, "jacksonCollection", collection);
        Whitebox.setInternalState(kb, "jacksonStatisticsCollection", statisticsCollection);
        indications = new LinkedList<Indication>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
        mockedCause = new FailureCause("id", "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(Metrics.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getPlugin(Metrics.class)).thenReturn(metricsPlugin);
        PowerMockito.when(metricsPlugin.metricRegistry()).thenReturn(metricRegistry);
    }

    /**
     * Tests finding one cause by its id.
     *
     * @throws Exception if so.
     */
    @Test
    public void testFindOneCause() throws Exception {
        when(collection.findOneById(anyString())).thenReturn(mockedCause);
        FailureCause fetchedCause = kb.getCause("id");
        assertNotNull("The fetched cause should not be null", fetchedCause);
        assertSame(mockedCause, fetchedCause);
    }

    /**
     * Tests finding all causes.
     *
     * @throws Exception if so.
     */
    @Test
    public void testGetCauseNames() throws Exception {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        MongoCursor<FailureCause> cursor = mock(MongoCursor.class);
        List<FailureCause> list = new LinkedList<FailureCause>();
        list.add(mockedCause);
        when(iterable.iterator()).thenReturn(cursor);
        when(cursor.next()).thenReturn(mockedCause);
        when(cursor.hasNext()).thenReturn(true, false);
        doReturn(iterable).when(collection).find(Matchers.<Bson>any());
        Collection<FailureCause> fetchedCauses = kb.getCauseNames();
        assertNotNull("The fetched cause should not be null", fetchedCauses);
        Iterator fetch = fetchedCauses.iterator();
        assertTrue(fetch.hasNext());
        assertSame(mockedCause, fetch.next());
    }

    /**
     * Tests adding one cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testAddCause() throws Exception {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        UpdateResult result = mock(UpdateResult.class);
        doReturn(result).when(collection).save(mockedCause);
        doReturn(iterable).when(collection).find(Matchers.<Bson>any());
        when(iterable.first()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        FailureCause addedCause = kb.addCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
    }

    /**
     * Tests saving one cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testSaveCause() throws Exception {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        UpdateResult result = mock(UpdateResult.class);
        doReturn(result).when(collection).save(mockedCause);
        doReturn(iterable).when(collection).find(Matchers.<Bson>any());
        when(iterable.first()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        FailureCause addedCause = kb.saveCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
        verify(metricRegistry, times(2)).counter(Mockito.anyString());
    }

    /**
     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
     *
     * @throws Exception if so.
     */
    @Test(expected = MongoException.class)
    public void testThrowMongo() throws Exception {
        when(collection.find(Matchers.<Bson>any())).thenThrow(MongoException.class);
        kb.getCauseNames();
    }
}
