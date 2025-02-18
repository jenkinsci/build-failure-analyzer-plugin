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


import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.test.utils.Whitebox;
import java.util.concurrent.TimeUnit;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.Answer;
import org.mongojack.JacksonMongoCollection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the Mongo cache.
 */
class MongoDBKnowledgeBaseCacheTest {

    /**
     * Tests that the cache can start, update itself and stop correctly.
     * @throws Exception if so.
     */
    @Test
    @Timeout(value = 15000, unit = TimeUnit.MILLISECONDS)
    void testStartStop() throws Exception {
        FailureCause mockedCause =
                new FailureCause("id", "myFailureCause", "description", "comment", null, "category", null, null);
        JacksonMongoCollection<FailureCause> collection = mock(JacksonMongoCollection.class);
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        when(iterable.iterator()).thenAnswer((Answer<MongoCursor<FailureCause>>)invocation -> {
            MongoCursor<FailureCause> cursor = mock(MongoCursor.class);
            when(cursor.next()).thenReturn(mockedCause);
            when(cursor.hasNext()).thenReturn(true, false);
            return cursor;
        });
        doReturn(iterable).when(collection).find(any(Bson.class));
        DistinctIterable<String> categoriesIterable = mock(DistinctIterable.class);
        when(categoriesIterable.iterator()).thenAnswer((Answer<MongoCursor<String>>)invocation -> {
            MongoCursor<String> categoriesCursor = mock(MongoCursor.class);
            when(categoriesCursor.next()).thenReturn("test");
            when(categoriesCursor.hasNext()).thenReturn(true, false);
            return categoriesCursor;
        });
        doReturn(categoriesIterable).when(collection).distinct("categories", String.class);
        MongoDBKnowledgeBaseCache cache = new MongoDBKnowledgeBaseCache(collection);
        cache.start();
        while (cache.getCauses() == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("Got interrupted while waiting for the cache to update.");
                break;
            }
        }
        List<FailureCause> list = cache.getCauses();
        assertNotNull(Whitebox.getInternalState(cache, "updaterThread"), "Updater thread should not be null");
        assertEquals(mockedCause, list.get(0), "Cache should have been updated with the correct cause");
        cache.stop();
        Thread.sleep(1000);
        assertNull(Whitebox.getInternalState(cache, "updaterThread"), "Updater thread should be null");
    }

    /**
     * Tests that the cache does not fail if the update thread has not run.
     */
    @Test
    void testUnStartedCacheStillReturnsData() {
        FailureCause mockedCause =
                new FailureCause("id", "myFailureCause", "description", "comment", null, "category", null, null);
        FindIterable<FailureCause> iterable = mock(FindIterable.class);
        MongoCursor<FailureCause> cursor = mock(MongoCursor.class);
        when(iterable.iterator()).thenReturn(cursor);
        when(cursor.next()).thenReturn(mockedCause);
        when(cursor.hasNext()).thenReturn(true, false);
        JacksonMongoCollection<FailureCause> collection = mock(JacksonMongoCollection.class);
        doReturn(iterable).when(collection).find(any(Bson.class));
        DistinctIterable<String> categoriesIterable = mock(DistinctIterable.class);
        MongoCursor<String> categoriesCursor = mock(MongoCursor.class);
        when(categoriesIterable.iterator()).thenReturn(categoriesCursor);
        when(categoriesCursor.next()).thenReturn("test");
        when(categoriesCursor.hasNext()).thenReturn(true, false);
        doReturn(categoriesIterable).when(collection).distinct("categories", String.class);
        MongoDBKnowledgeBaseCache cache = new MongoDBKnowledgeBaseCache(collection);
        List<FailureCause> list = cache.getCauses();
        assertEquals(mockedCause, list.get(0), "Cache should have been updated with the correct cause");
        List<String> categoriesList = cache.getCategories();
        assertEquals("test", categoriesList.get(0), "Cache should have been updated with the correct category");
    }
}
