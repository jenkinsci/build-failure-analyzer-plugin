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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.connection.ClusterConnectionMode;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mongojack.JacksonMongoCollection;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for the MongoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
class MongoDBKnowledgeBaseTest {

    private JacksonMongoCollection<FailureCause> collection;
    private JacksonMongoCollection<DBObject> statisticsCollection;
    private MongoDBKnowledgeBase kb;
    private List<Indication> indications;
    private Indication indication;
    private FailureCause mockedCause;
    private static final int PORT = 27017;

    private Jenkins jenkins;
    private Metrics metricsPlugin;
    private MetricRegistry metricRegistry;
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<Metrics> metricsMockedStatic;

    /**
     * Common stuff to set up for the tests.
     */
    @BeforeEach
    void setUp() {
        jenkins = mock(Jenkins.class);
        metricsPlugin = mock(Metrics.class);
        metricRegistry = mock(MetricRegistry.class);
        kb = new MongoDBKnowledgeBase("", PORT, "mydb", null, null, false, false);
        collection = mock(JacksonMongoCollection.class);
        statisticsCollection = mock(JacksonMongoCollection.class);
        Whitebox.setInternalState(kb, "jacksonCollection", collection);
        Whitebox.setInternalState(kb, "jacksonStatisticsCollection", statisticsCollection);

        indications = new LinkedList<>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
        mockedCause = new FailureCause("id", "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);

        jenkinsMockedStatic = mockStatic(Jenkins.class);
        metricsMockedStatic = mockStatic(Metrics.class);
        jenkinsMockedStatic.when(Jenkins::get).thenReturn(jenkins);
        when(jenkins.getPlugin(Metrics.class)).thenReturn(metricsPlugin);
        metricsMockedStatic.when(Metrics::metricRegistry).thenReturn(metricRegistry);
    }

    /**
     * Release all the static mocks.
     */
    @AfterEach
    void tearDown() {
        jenkinsMockedStatic.close();
        metricsMockedStatic.close();
    }

    /**
     * Tests that the cluster connection mode is set correctly for each kind of input.
     *
     */
    @Test
    void testClusterModes() {
        MongoDBKnowledgeBase singleNodekb = new MongoDBKnowledgeBase(
                "oneNode", PORT, "mydb", null, null, false, false);
        MongoDBKnowledgeBase multiNodekb = new MongoDBKnowledgeBase(
                "node1,node2,node3", PORT, "mydb", null, null, false, false);
        MongoClient mongo = singleNodekb.getMongoConnection();
        ClusterConnectionMode connectionMode = mongo.getClusterDescription().getConnectionMode();
        assertSame(ClusterConnectionMode.SINGLE, connectionMode);
        mongo = multiNodekb.getMongoConnection();
        connectionMode = mongo.getClusterDescription().getConnectionMode();
        assertSame(ClusterConnectionMode.MULTIPLE, connectionMode);
    }

    /**
     * Tests finding one cause by its id.
     *
     */
    @Test
    void testFindOneCause() {
        when(collection.findOneById(anyString())).thenReturn(mockedCause);
        FailureCause fetchedCause = kb.getCause("id");
        assertNotNull(fetchedCause, "The fetched cause should not be null");
        assertSame(mockedCause, fetchedCause);
    }

    /**
     * Tests finding all causes.
     *
     */
    @Test
    void testGetCauseNames() {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        MongoCursor<FailureCause> cursor = mock(MongoCursor.class);
        List<FailureCause> list = new LinkedList<>();
        list.add(mockedCause);
        when(iterable.iterator()).thenReturn(cursor);
        when(cursor.next()).thenReturn(mockedCause);
        when(cursor.hasNext()).thenReturn(true, false);
        doReturn(iterable).when(collection).find(ArgumentMatchers.<Bson>any());
        Collection<FailureCause> fetchedCauses = kb.getCauseNames();
        assertNotNull(fetchedCauses, "The fetched cause should not be null");
        Iterator<FailureCause> fetch = fetchedCauses.iterator();
        assertTrue(fetch.hasNext());
        assertSame(mockedCause, fetch.next());
    }

    /**
     * Tests adding one cause.
     *
     */
    @Test
    void testAddCause() {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        UpdateResult result = mock(UpdateResult.class);
        doReturn(result).when(collection).save(mockedCause);
        doReturn(iterable).when(collection).find(ArgumentMatchers.<Bson>any());
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
     */
    @Test
    void testSaveCause() {
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        UpdateResult result = mock(UpdateResult.class);
        doReturn(result).when(collection).save(mockedCause);
        doReturn(iterable).when(collection).find(ArgumentMatchers.<Bson>any());
        when(iterable.first()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        FailureCause addedCause = kb.saveCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
        verify(metricRegistry, times(2)).counter(anyString());
    }

    /**
     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
     *
     */
    @Test
    void testThrowMongo() {
        when(collection.find(ArgumentMatchers.<Bson>any())).thenThrow(MongoException.class);
        assertThrows(MongoException.class, () ->
            kb.getCauseNames());
    }

    /**
     * Tests that the MongoConnection of the KnowledgeBase is set to null after stop is run.
     */
    @Test
    void testStopKnowledgeBase() {
        kb.getMongoConnection();
        kb.stop();
        assertNull(Whitebox.getInternalState(kb, "mongo"), "MongoConnection should be null");
    }
}
