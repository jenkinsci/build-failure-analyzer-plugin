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


import com.mongodb.DBObject;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the Mongo cache.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JacksonDBCollection.class)
public class MongoDBKnowledgeBaseCacheTest {

    /**
     * Tests that the cache can start, update itself and stop correctly.
     * @throws Exception if so.
     */
    @Test(timeout = 5000)
    public void testStartStop() throws Exception {
        FailureCause mockedCause =
                new FailureCause("id", "myFailureCause", "description", "comment", null, "category", null, null);
        DBCursor<FailureCause> cursor = mock(DBCursor.class);
        JacksonDBCollection<FailureCause, String> collection = mock(JacksonDBCollection.class);
        when(cursor.next()).thenReturn(mockedCause);
        when(cursor.hasNext()).thenReturn(true, false);
        doReturn(cursor).when(collection).find(any(DBObject.class));
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
        assertNotNull("Updater thread should not be null", Whitebox.getInternalState(cache, "updaterThread"));
        assertEquals("Cache should have been updated with the correct cause", mockedCause, list.get(0));
        cache.stop();
        Thread.sleep(1000);
        assertNull("Updater thread should be null", Whitebox.getInternalState(cache, "updaterThread"));
    }
}
