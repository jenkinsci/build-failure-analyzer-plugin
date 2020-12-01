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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mongojack.JacksonMongoCollection;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


/**
 * Tests for the MongoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JacksonMongoCollection.class)
public class MongoDBKnowledgeBaseTest {

    private JacksonMongoCollection<FailureCause> collection;
    JacksonMongoCollection<DBObject> jacksonStatisticsCollection;
    private JacksonMongoCollection<DBObject> statisticsCollection;
    private MongoDBKnowledgeBase kb;
    private List<Indication> indications;
    private Indication indication;
    private FailureCause mockedCause;
    private DBObject mockedStatistics;
    private static final int PORT = 27017;

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
        mockedStatistics = new BasicDBObject("key", "value");

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
        doReturn(iterable).when(collection).find(Matchers.<Bson>any());
        when(iterable.first()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        FailureCause addedCause = kb.saveCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
    }

    /**
     * Tests fetching statistics.
     *
     * @throws Exception if unable to fetch statistics.
     */
    @Test
    public void testGetStatistics() throws Exception {
        FindIterable<DBObject> iterable = mock(FindIterable.class);
        MongoCursor<DBObject> iterator = mock(MongoCursor.class);
        doReturn(iterable).when(statisticsCollection).find(Matchers.<Bson>any());
        when(iterable.limit(anyInt())).thenReturn(iterable);
        when(iterable.sort(any(Bson.class))).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(mockedStatistics);
        List<DBObject> fetchedStatistics = kb.getStatistics(null, 1);
        assertNotNull("The fetched statistics should not be null", fetchedStatistics);
        assertFalse("The fetched statistics list should not be empty", fetchedStatistics.isEmpty());
        assertSame(mockedStatistics, fetchedStatistics.get(0));
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
