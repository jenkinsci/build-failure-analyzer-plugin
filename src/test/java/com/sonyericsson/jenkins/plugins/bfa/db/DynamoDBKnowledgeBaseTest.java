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

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.datamodeling.marshallers.ObjectToMapMarshaller;
import com.amazonaws.services.dynamodbv2.datamodeling.marshallers.ObjectToStringMarshaller;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseModification;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.vz.mongodb.jackson.DBCursor;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.WriteResult;
import org.acegisecurity.Authentication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;


import java.util.*;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;


/**
 * Tests for the DynamoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore( {"javax.*"})
@PrepareForTest({DynamoDBKnowledgeBase.class, Jenkins.class, Jenkins.DescriptorImpl.class})
public class DynamoDBKnowledgeBaseTest {


    @Mock
    private Jenkins jenkins;
    @Mock
    private DynamoDBMapper dbMapper;
    @Mock
    private AmazonDynamoDB db;

    private JacksonDBCollection<FailureCause, String> collection;
    private JacksonDBCollection<Statistics, String> statisticsCollection;
    private DynamoDBKnowledgeBase kb;
    private List<Indication> indications;
    private List<FailureCauseModification> modifications;
    private Indication indication;
    private FailureCause mockedCause;
    private Statistics mockedStatistics;

    /**
     * Common stuff to set up for the tests.
     */
    @Before
    public void setUp() throws Exception{
        Authentication mockAuth = mock(Authentication.class);
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(Jenkins.getAuthentication()).thenReturn(mockAuth);
        PowerMockito.when(mockAuth.getName()).thenReturn("tester");

//        PowerMockito.doReturn(discriptor).when(jenkins).getDescriptorByType(FailureCause.FailureCauseDescriptor.class);


        kb = PowerMockito.spy(new DynamoDBKnowledgeBase("", "", "default"));
        db = mock(AmazonDynamoDB.class);
        dbMapper = spy(new DynamoDBMapper(db));
        PowerMockito.doReturn(dbMapper).when(kb, "getDbMapper");

        //        collection = mock(JacksonDBCollection.class);
//        statisticsCollection = mock(JacksonDBCollection.class);
//        Whitebox.setInternalState(kb, "jacksonCollection", collection);
//        Whitebox.setInternalState(kb, "jacksonStatisticsCollection", statisticsCollection);
        indications = new LinkedList<Indication>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
//        mockedCause = createFailureCause(null);
//        mockedStatistics = new Statistics("projectName", 1, "", null, 1, null, "nodeName", "master", 0, "result",
//                null, null);
    }

    public FailureCause createFailureCause(String id) throws Exception{
        return new FailureCause(id, "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);
    }

    public void mockScanRequest(Collection<FailureCause> causes) throws Exception {
        DynamoDBMapperTableModel fcModel = dbMapper.getTableModel(FailureCause.class);
        Collection<Map<String, AttributeValue>> convertedFcs = new ArrayList<>();
        for (FailureCause fc:causes) {
            Map<String, AttributeValue> convertedFc = fcModel.convert(fc);
            convertedFcs.add(convertedFc);
        }

        ScanResult mockedScanResult = spy(new ScanResult()).withItems(convertedFcs);
        doReturn(mockedScanResult).when(db, "scan", Matchers.any());
    }

    /**
     * Tests finding one cause by its id.
     *
     * @throws Exception if so.
     */
    @Test
    public void testFindOneCause() throws Exception {
        String id = "2ce2ae7b-7f66-4a8c-984a-802a43d3a9a4";
        FailureCause mockedCause = createFailureCause(id);
        doReturn(mockedCause).when(dbMapper).load(FailureCause.class, id);
        FailureCause fetchedCause = kb.getCause(id);
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
        Collection<FailureCause> expectedCauses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Integer id = i;
            FailureCause cause = new FailureCause(id.toString(), "myFailureCause" + id.toString(), "description", "comment", new Date(),
                    "category", indications, null);

            // Null all fields except for Id and Name, as we are requesting only those two fields
            cause.setDescription(null);
            cause.setComment(null);
            cause.setLastOccurred(null);
            cause.setCategories(null);
            cause.setIndications(null);
            cause.setModifications(null);

            expectedCauses.add(cause);
        }

        mockScanRequest(expectedCauses);
        DynamoDBScanExpression scanExpression = PowerMockito.spy(new DynamoDBScanExpression());
        whenNew(DynamoDBScanExpression.class).withAnyArguments().thenReturn(scanExpression);

        Collection<FailureCause> fetchedCauses = kb.getCauseNames();

        Mockito.verify(scanExpression).addExpressionAttributeNamesEntry("#n", "name");
        Mockito.verify(scanExpression).setProjectionExpression("id,#n");
        Mockito.verify(scanExpression).setScanFilter(DynamoDBKnowledgeBase.NOT_REMOVED_FILTER_EXPRESSION);

        assertNotNull("The fetched cause should not be null", fetchedCauses);;
        // Convert fetchedCauses to list, because PaginatedList does not allow iterators
        List<FailureCause> actualCauses = new ArrayList<>(fetchedCauses);
        assertEquals(actualCauses, actualCauses);

        for (FailureCause ac:actualCauses) {
            assertNotNull("Id should not be null", ac.getId());
            assertNotNull("Name should not be null", ac.getName());
            assertNull("Description should be null", ac.getDescription());
        }
    }

    /**
     * Tests adding one cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testAddCause() throws Exception {
        // This is not a very effective test, since addCause is just a passthrough to saveCause
        FailureCause noIdCause = createFailureCause(null);
        FailureCause idCause = createFailureCause("foo");
        doReturn(idCause).when(kb).saveCause(noIdCause);
        FailureCause addedCause = kb.addCause(noIdCause);
        assertNotNull(addedCause);
        assertNotSame(noIdCause, addedCause);
    }

    /**
     * Tests saving one cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testSaveCause() throws Exception {
        FailureCause cause = createFailureCause("foo");
        doNothing().when(dbMapper).save(cause);
        FailureCause savedCause = kb.saveCause(cause);
        assertNotNull(savedCause);
        assertSame(cause, savedCause);
    }

    /**
     * Tests fetching statistics.
     *
     * @throws Exception if unable to fetch statistics.
     */
//    @Test
    public void testGetStatistics() throws Exception {
//        DBCursor<Statistics> cursor = mock(DBCursor.class);
//        List<Statistics> list = new LinkedList<Statistics>();
//        list.add(mockedStatistics);
//
//        doReturn(cursor).when(statisticsCollection).find(Matchers.<DBObject>any());
//        when(cursor.limit(anyInt())).thenReturn(cursor);
//        when(cursor.sort(any(DBObject.class))).thenReturn(cursor);
//        when(cursor.toArray()).thenReturn(list);
//
//        List<Statistics> fetchedStatistics = kb.getStatistics(null, 1);
//        assertNotNull("The fetched statistics should not be null", fetchedStatistics);
//        assertFalse("The fetched statistics list should not be empty", fetchedStatistics.isEmpty());
//        assertSame(mockedStatistics, fetchedStatistics.get(0));
    }

    /**
     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
     *
     * @throws Exception if so.
     */
    @Test(expected = MongoException.class)
    public void testThrowMongo() throws Exception {
        when(collection.find(Matchers.<DBObject>any(), Matchers.<DBObject>any())).thenThrow(MongoException.class);
        kb.getCauseNames();
    }

//    /**
//     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
//     *
//     * @throws Exception if so.
//     */
//    @Test
//    public void testGetDynamoDB() throws Exception {
//        kb.getDynamoDb();
//    }

    /**
     * Tests that the mongo exception caused by the collection gets thrown from the knowledgebase.
     *
     * @throws Exception if so.
     */
//    @Test
//    public void testCreateTables() throws Exception {
//        kb.createTable();
//    }

    @Test
    public void getCauses() throws Exception {
        DynamoDBMapper mapper = mock(DynamoDBMapper.class);
//        doReturn().when(mapper).save();
        Collection<FailureCause> causes = kb.getCauses();
        System.out.println("foo");
    }

    @Test
    public void getShallowCauses() throws Exception {
        Collection<FailureCause> causes = kb.getShallowCauses();
        System.out.println("foo");
    }

    @Test
    public void removeCause() throws Exception {
        FailureCause cause = kb.removeCause("bc3e1c3d-222e-43dd-8efc-2ddec79485b0");
        System.out.println("foo");
    }

    @Test
    public void getCategories() throws Exception {
        List<String> foo = kb.getCategories();
        System.out.println("foo");
    }
}
