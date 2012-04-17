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

import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import net.vz.mongodb.jackson.DBCursor;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


/**
 * Tests for the MongoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JacksonDBCollection.class)
public class MongoDBKnowledgeBaseTest {

    private JacksonDBCollection<FailureCause, String> collection;
    private MongoDBKnowledgeBase kb;
    private List<Indication> indications;
    private Indication indication;
    private FailureCause mockedCause;
    private static final int PORT = 27017;

    /**
     * Common stuff to set up for the tests.
     */
    @Before
    public void setUp() {
        kb = new MongoDBKnowledgeBase("", PORT, "mydb", null, null, false);
        collection = mock(JacksonDBCollection.class);
        Whitebox.setInternalState(kb, collection);
        indications = new LinkedList<Indication>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
        mockedCause = new FailureCause("id", "myFailureCause", "description", "category", indications);
    }

    /**
     * Tests finding one cause by its id.
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
     * @throws Exception if so.
     */
    @Test
    public void testGetCauseNames() throws Exception {
        DBCursor<FailureCause> cursor = mock(DBCursor.class);
        Whitebox.setInternalState(kb, collection);
        List<FailureCause> list = new LinkedList<FailureCause>();
        list.add(mockedCause);
        when(cursor.next()).thenReturn(mockedCause);
        when(cursor.hasNext()).thenReturn(true, false);
        doReturn(cursor).when(collection).find(Matchers.<DBObject>any(), Matchers.<DBObject>any());
        Collection<FailureCause> fetchedCauses = kb.getCauseNames();
        assertNotNull("The fetched cause should not be null", fetchedCauses);
        Iterator fetch = fetchedCauses.iterator();
        assertTrue(fetch.hasNext());
        assertSame(mockedCause, fetch.next());
    }

    /**
     * Tests adding one cause.
     * @throws Exception if so.
     */
    @Test
    public void testAddCause() throws Exception {
        WriteResult<FailureCause, String> result = mock(WriteResult.class);
        when(result.getSavedObject()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        doReturn(result).when(collection).insert(Matchers.<FailureCause>any());
        FailureCause addedCause = kb.addCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
    }

    /**
     * Tests saving one cause.
     * @throws Exception if so.
     */
    @Test
    public void testSaveCause() throws Exception {
        WriteResult<FailureCause, String> result = mock(WriteResult.class);
        when(result.getSavedObject()).thenReturn(mockedCause);
        MongoDBKnowledgeBaseCache cache = mock(MongoDBKnowledgeBaseCache.class);
        Whitebox.setInternalState(kb, cache);
        doReturn(result).when(collection).save(Matchers.<FailureCause>any());
        FailureCause addedCause = kb.saveCause(mockedCause);
        assertNotNull(addedCause);
        assertSame(mockedCause, addedCause);
    }

    /**
     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
     * @throws Exception if so.
     */
    @Test(expected = MongoException.class)
    public void testThrowMongo() throws Exception {
        when(collection.find(Matchers.<DBObject>any(), Matchers.<DBObject>any())).thenThrow(MongoException.class);
        kb.getCauseNames();
    }
}
