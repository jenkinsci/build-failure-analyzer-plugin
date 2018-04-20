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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.collections.CollectionUtils;
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


import java.util.LinkedList;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Date;


import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doNothing;



/**
 * Tests for the DynamoDBKnowledgeBase.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.*"})
@PrepareForTest({DynamoDBKnowledgeBase.class, Jenkins.class, Jenkins.DescriptorImpl.class})
public class DynamoDBKnowledgeBaseTest {

    @Mock
    private Jenkins jenkins;
    @Mock
    private DynamoDBMapper dbMapper;
    @Mock
    private AmazonDynamoDB db;

    private DynamoDBKnowledgeBase kb;
    private List<Indication> indications;
    private Indication indication;
    private Statistics mockedStatistics;
    private String mockJenkinsUserName = "Jtester";
    private static final int MAX_ITERATIONS = 3;

    /**
     * Common stuff to set up for the tests.
     * @throws Exception if so.
     */
    @Before
    public void setUp() throws Exception{
        // Mock interactions with Jenkins
        Authentication mockAuth = mock(Authentication.class);
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(Jenkins.getAuthentication()).thenReturn(mockAuth);
        PowerMockito.when(mockAuth.getName()).thenReturn(mockJenkinsUserName);

        // Mock DynamoDB and DBMapper
        kb = PowerMockito.spy(new DynamoDBKnowledgeBase("", "", "default"));
        db = mock(AmazonDynamoDB.class);
        dbMapper = spy(new DynamoDBMapper(db));
        PowerMockito.doReturn(dbMapper).when(kb, "getDbMapper");

        indications = new LinkedList<Indication>();
        indication = new BuildLogIndication("something");
        indications.add(indication);
    }

    /**
     * Helper to create a FailureCause during testing.
     * @param id string id FailureCause to return
     * @return FailureCause
     * @throws Exception if so.
     */
    public FailureCause createFailureCause(String id) throws Exception{
        return new FailureCause(id, "myFailureCause", "description", "comment", new Date(),
                "category", indications, null);
    }

    /**
     * Sets up a mock scan result, which DynamoDB uses when creating a paginated list of results.
     * @param causes collection of FailureCauses
     * @throws Exception if so.
     */
    public void mockScanResult(Collection<FailureCause> causes) throws Exception {
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
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Integer id = i;
            FailureCause cause = new FailureCause(id.toString(),
                    "myFailureCause" + id.toString(), "description", "comment", new Date(),
                    "category", indications, null);
            expectedCauses.add(cause);
        }

        mockScanResult(expectedCauses);
        DynamoDBScanExpression scanExpression = PowerMockito.spy(new DynamoDBScanExpression());
        whenNew(DynamoDBScanExpression.class).withAnyArguments().thenReturn(scanExpression);

        Collection<FailureCause> fetchedCauses = kb.getCauseNames();

        // Ensure the scan filtering is actually called
        Mockito.verify(scanExpression).addExpressionAttributeNamesEntry("#n", "name");
        Mockito.verify(scanExpression).setProjectionExpression("id,#n");
        Mockito.verify(scanExpression).setScanFilter(DynamoDBKnowledgeBase.NOT_REMOVED_FILTER_EXPRESSION);

        assertNotNull("The fetched cause should not be null", fetchedCauses);
        // Convert fetchedCauses to list, because PaginatedList does not allow iterators
        List<FailureCause> actualCauses = new ArrayList<>(fetchedCauses);
        assertTrue(expectedCauses.equals(actualCauses));
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
     * Tests getting a the shallow form of causes.
     * @throws Exception if so
     */
    @Test
    public void testGetShallowCauses() throws Exception {
        Collection<FailureCause> expectedCauses = new ArrayList<>();
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Integer id = i;
            FailureCause cause = createFailureCause(id.toString());
            expectedCauses.add(cause);
        }

        mockScanResult(expectedCauses);
        DynamoDBScanExpression scanExpression = PowerMockito.spy(new DynamoDBScanExpression());
        whenNew(DynamoDBScanExpression.class).withAnyArguments().thenReturn(scanExpression);

        Collection<FailureCause> fetchedCauses = kb.getShallowCauses();
        Mockito.verify(scanExpression).addExpressionAttributeNamesEntry("#n", "name");
        Mockito.verify(scanExpression).addExpressionAttributeNamesEntry("#c", "comment");
        Mockito.verify(scanExpression).addExpressionAttributeNamesEntry("#r", "_removed");
        Mockito.verify(scanExpression)
                .setProjectionExpression("id,#n,description,categories,#c,modifications,lastOccurred");
        Mockito.verify(scanExpression).setFilterExpression(" attribute_not_exists(#r) ");

        assertNotNull("The fetched cause should not be null", fetchedCauses);
        // Convert fetchedCauses to list, because PaginatedList does not allow iterators
        List<FailureCause> actualCauses = new ArrayList<>(fetchedCauses);
        assertEquals(expectedCauses, actualCauses);
    }

    /**
     * Test that the cause gets updated with removed info.
     * @throws Exception if so.
     */
    @Test
    public void testRemoveCause() throws Exception {
        String id = "test";
        FailureCause cause = createFailureCause(id);

        doReturn(cause).when(dbMapper).load(FailureCause.class, id);
        doNothing().when(dbMapper).save(cause);
        FailureCause actualCause = kb.removeCause(id);
        assertNotNull(actualCause);
        assertSame(cause, actualCause);
        assertNotNull(actualCause.getRemoved());
        assertEquals(mockJenkinsUserName, actualCause.getRemoved().get("by"));
    }

    /**
     * Tests getting {@link FailureCause} categories.
     * @throws Exception if so
     */
    @Test
    public void testGetCategories() throws Exception {
        List<String> expectedCategories = new ArrayList<>();
        expectedCategories.add("category0");
        expectedCategories.add("category1");
        expectedCategories.add("category2");

        Collection<FailureCause> causes = new ArrayList<>();
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Integer id = i;
            FailureCause cause = new FailureCause(id.toString(),
                    "myFailureCause" + id.toString(), "description", "comment", new Date(),
                    "category" + id.toString(), indications, null);
            causes.add(cause);
        }

        mockScanResult(causes);
        DynamoDBScanExpression scanExpression = PowerMockito.spy(new DynamoDBScanExpression());
        whenNew(DynamoDBScanExpression.class).withAnyArguments().thenReturn(scanExpression);

        List<String> actualCategories = kb.getCategories();
        Mockito.verify(scanExpression).setProjectionExpression("categories");
        Mockito.verify(scanExpression).setFilterExpression(" attribute_exists(categories) ");
        assertNotNull(actualCategories);

        // Values should be the same, but might be in different order
        assertTrue(CollectionUtils.isEqualCollection(expectedCategories, actualCategories));
    }

    /**
     * Tests converting {@link FailureCause} from a different {@link KnowledgeBase} type.
     * @throws Exception if so
     */
    @Test
    public void testConvertFrom() throws Exception {
        LocalFileKnowledgeBase localKb = spy(new LocalFileKnowledgeBase());
        Collection<FailureCause> causes = new ArrayList<>();
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Integer id = i;
            FailureCause cause = createFailureCause(id.toString());
            causes.add(cause);
        }

        doReturn(causes).when(localKb).getCauseNames();
        doReturn(createFailureCause("foo")).when(kb).saveCause(Matchers.any(FailureCause.class));
        kb.convertFrom(localKb);
        Mockito.verify(kb, Mockito.times(MAX_ITERATIONS)).saveCause(Matchers.any(FailureCause.class));
    }
}
