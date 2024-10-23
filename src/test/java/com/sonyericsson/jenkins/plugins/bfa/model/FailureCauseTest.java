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

package com.sonyericsson.jenkins.plugins.bfa.model;

import com.codahale.metrics.MetricRegistry;
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Failure;
import hudson.util.FormValidation;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FailureCause}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class FailureCauseTest {

    private PluginImpl pluginMock;
    private KnowledgeBase baseMock;
    private FailureCause.FailureCauseDescriptor descriptor;


    private Jenkins jenkinsMock;
    private Metrics metricsPlugin;
    @Mock
    private MetricRegistry metricRegistry;
    private MockedStatic<PluginImpl> pluginMockedStatic;
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<Metrics> metricsMockedStatic;

    /**
     * Runs before every test.
     * Mocks {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getInstance()} to avoid NPE's
     * when checking permissions in the code under test.
     */
    @Before
    public void setUp() {
        jenkinsMock = mock(Jenkins.class);
        metricsPlugin = mock(Metrics.class);
        pluginMock = mock(PluginImpl.class);
        pluginMockedStatic = mockStatic(PluginImpl.class);
        pluginMockedStatic.when(PluginImpl::getInstance).thenReturn(pluginMock);

        jenkinsMockedStatic = mockStatic(Jenkins.class);
        jenkinsMockedStatic.when(Jenkins::getInstance).thenReturn(jenkinsMock);
        jenkinsMockedStatic.when(() -> Jenkins.checkGoodName(any())).thenCallRealMethod();

        descriptor = new FailureCause.FailureCauseDescriptor();
        when(jenkinsMock.getDescriptorByType(FailureCause.FailureCauseDescriptor.class)).thenReturn(descriptor);

        metricsMockedStatic = mockStatic(Metrics.class);
        when(jenkinsMock.getPlugin(Metrics.class)).thenReturn(metricsPlugin);
        metricsMockedStatic.when(Metrics::metricRegistry).thenReturn(metricRegistry);
    }

    /**
     * Release all the static mocks.
     */
    @After
    public void tearDown() {
        pluginMockedStatic.close();
        jenkinsMockedStatic.close();
        metricsMockedStatic.close();
    }

    /**
     * Puts a simple plain mock to be returned from
     * {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getKnowledgeBase()}.
     * All tests doesn't need this or needs a more complicated mock.
     */
    private void mockEmptyKnowledgeBase() {
        baseMock = mock(KnowledgeBase.class);
        when(pluginMock.getKnowledgeBase()).thenReturn(baseMock);
    }

    /**
     * Tests that the auto completion can find the correct canditates.
     * @throws Exception if so.
     */
    @Test
    public void testAutoCompletionHappy() throws Exception {
        mockEmptyKnowledgeBase();
        when(pluginMock.getCategoryAutoCompletionCandidates(any())).thenCallRealMethod();
        List<String> categories = new LinkedList<String>();
        String compFail = "compilationFailure";
        String compCrashed = "computerCrashed";
        String otherError = "otherError";
        categories.add(compFail);
        categories.add(compCrashed);
        categories.add(otherError);
        when(baseMock.getCategories()).thenReturn(categories);
        AutoCompletionCandidates candidates = descriptor.doAutoCompleteCategories("comp");
        List<String> values = candidates.getValues();
        assertEquals("Two autocompletion candidates should have been found", 2, values.size());
        assertThat(values, hasItems(compFail, compCrashed));
    }

    /**
     * Test for {@link FailureCause#validate(String, String, java.util.List)}.
     * With missing name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testValidateBadName() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validate = cause.validate("", "description", Collections.EMPTY_LIST);
        assertSame(FormValidation.Kind.ERROR, validate.kind);
    }

    /**
     * Test for {@link FailureCause#validate(String, String, java.util.List)}.
     * With missing description.
     *
     * @throws Exception if so.
     */
    @Test
    public void testValidateBadDescription() throws Exception {
        mockEmptyKnowledgeBase();
        FailureCause cause = new FailureCause();
        FormValidation validate = cause.validate("Some Name", "", Collections.EMPTY_LIST);
        assertSame(FormValidation.Kind.ERROR, validate.kind);
    }

    /**
     * Test for {@link FailureCause#validate(String, String, java.util.List)}.
     * With missing indications.
     *
     * @throws Exception if so.
     */
    @Test
    public void testValidateNoIndications() throws Exception {
        mockEmptyKnowledgeBase();
        FailureCause cause = new FailureCause();
        FormValidation validate = cause.validate("Some Name", "The Description", Collections.EMPTY_LIST);
        assertSame(FormValidation.Kind.ERROR, validate.kind);
    }

    /**
     * Test for {@link FailureCause#validate(String, String, java.util.List)}.
     * With an indication that won't validate but the rest is ok.
     *
     * @throws Exception if so.
     */
    @Test
    public void testValidateBadIndication() throws Exception {
        mockEmptyKnowledgeBase();
        FailureCause cause = new FailureCause();
        Indication indication = new BuildLogIndication("some(thing");
        FormValidation validate =
                cause.validate("Some Name", "The Description", Collections.singletonList(indication));
        assertSame(FormValidation.Kind.ERROR, validate.kind);
    }

    /**
     * Happy test for {@link FailureCause#validate(String, String, java.util.List)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testValidate() throws Exception {
        mockEmptyKnowledgeBase();
        FailureCause cause = new FailureCause();
        Indication indication = new BuildLogIndication(".*");
        FormValidation validate =
                cause.validate("Some Name", "The Description", Collections.singletonList(indication));
        assertSame(FormValidation.Kind.OK, validate.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckDescription(String)} with an empty description.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescriptionEmpty() throws Exception {
        FormValidation validation = descriptor.doCheckDescription("");
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckDescription(String)}.
     * With the reserved "new" description ({@link CauseManagement#NEW_CAUSE_DESCRIPTION}).
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescriptionReserved() throws Exception {
        FormValidation validation = descriptor.doCheckDescription(CauseManagement.NEW_CAUSE_DESCRIPTION);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause.FailureCauseDescriptor#doCheckDescription(String)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescription() throws Exception {
        FormValidation validation = descriptor.doCheckDescription("My <b>Description</b>");
        assertSame(FormValidation.Kind.OK, validation.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckName(String, String)} with an empty name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameEmpty() throws Exception {
        FormValidation validation = descriptor.doCheckName("", null);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckName(String, String)}
     * with the reserved "new" name ({@link CauseManagement#NEW_CAUSE_NAME}).
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameReserved() throws Exception {
        FormValidation validation = descriptor.doCheckName(CauseManagement.NEW_CAUSE_NAME, null);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckName(String, String)} with bad characters in the name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameNoneGood() throws Exception {
        FormValidation validation = descriptor.doCheckName("[Name]", null);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause.FailureCauseDescriptor#doCheckName(String, String)} when
     * there already exists a cause with the same name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameExisting() throws Exception {
        List<FailureCause> initial = new LinkedList<FailureCause>();
        FailureCause other =
                new FailureCause("abc", "AName", "description", "comment", null, "", Collections.EMPTY_LIST, null);
        initial.add(other);
        other = new FailureCause("cde", "BName", "description", "comment", null, "", Collections.EMPTY_LIST, null);
        initial.add(other);
        LocalFileKnowledgeBase base = new LocalFileKnowledgeBase(initial);

        when(pluginMock.getKnowledgeBase()).thenReturn(base);

        FormValidation validation = descriptor.doCheckName("BName", null);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause.FailureCauseDescriptor#doCheckName(String, String)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckName() throws Exception {
        LocalFileKnowledgeBase base = new LocalFileKnowledgeBase();
        when(pluginMock.getKnowledgeBase()).thenReturn(base);
        FormValidation validation = descriptor.doCheckName("Some name", null);
        assertSame(FormValidation.Kind.OK, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)}. Where the intention is to save a new cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoConfigSubmitNewOk() throws Exception {

        mockEmptyKnowledgeBase();

        String name = "AName";
        String description = "The Description";
        String comment = "Comment";
        String category = "category";
        String pattern = ".*";
        StaplerRequest2 request = mockRequest("", name, description, comment, null, category,
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse2 response = mock(StaplerResponse2.class);

        FailureCause cause = new FailureCause(null, CauseManagement.NEW_CAUSE_NAME,
                CauseManagement.NEW_CAUSE_DESCRIPTION, null, null, category, null, null);
        cause.doConfigSubmit(request, response);

        verify(baseMock).addCause(same(cause));
        assertEquals(name, cause.getName());
        assertEquals(description, cause.getDescription());
        assertEquals(category, cause.getCategoriesAsString());
        assertEquals(1, cause.getIndications().size());
        assertEquals(pattern, cause.getIndications().get(0).getPattern().pattern());
        verify(response).sendRedirect2(any(String.class));
    }

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)}. Where the intention is to save changes to an existing cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoConfigSubmitSaveOk() throws Exception {
        mockEmptyKnowledgeBase();

        String id = "abc";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest2 request = mockRequest(id, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse2 response = mock(StaplerResponse2.class);

        FailureCause cause = new FailureCause("Old Name", "Old Description");
        cause.setId(id);
        cause.doConfigSubmit(request, response);

        verify(baseMock).saveCause(same(cause));
        assertEquals(id, cause.getId());
        assertEquals(name, cause.getName());
        assertEquals(description, cause.getDescription());
        assertEquals(1, cause.getIndications().size());
        assertEquals(pattern, cause.getIndications().get(0).getPattern().pattern());
        verify(response).sendRedirect2(any(String.class));
    }

    /**
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)} with a different id in the form than what the stapler binding says it is.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitSaveWrongId() throws Exception {
        mockEmptyKnowledgeBase();

        String origId = "abc";
        String newId = "cde";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest2 request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse2 response = mock(StaplerResponse2.class);

        FailureCause cause = new FailureCause("Old Name", "Old Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)} where the form data for id is set when it should not be since it is for a
     * new cause.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitNewWithId() throws Exception {
        mockEmptyKnowledgeBase();

        String origId = "";
        String newId = "cde";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest2 request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse2 response = mock(StaplerResponse2.class);

        FailureCause cause = new FailureCause("Old Name", "Old Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest2,
     * org.kohsuke.stapler.StaplerResponse2)} where the form data for id is null when it should be for an existing
     * cause.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitNewCloneAttempt() throws Exception {
        mockEmptyKnowledgeBase();

        String origId = "abc";
        String newId = "";
        String name = "New Name";
        String description = "New Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest2 request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse2 response = mock(StaplerResponse2.class);

        FailureCause cause = new FailureCause("A Name", "The Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Mocks a request to contain the form data for a {@code FailureCause} with the provided content.
     *
     * @param id          the id of the cause
     * @param name        the name
     * @param description the description
     * @param comment     the comment
     * @param occurred    the time of last occurrence
     * @param category    the category
     * @param indications the list of indications as they should be returned from
     *                      {@link StaplerRequest2#bindJSONToList(Class, Object)}
     * @param modifications the modification history of this FailureCause
     * @return a mocked request object.
     *
     * @throws ServletException if so, but probably not.
     */
    private StaplerRequest2 mockRequest(String id, String name, String description, String comment, Date occurred,
                                       String category, List<? extends Indication> indications,
                                       List<FailureCauseModification> modifications) throws ServletException {
        JSONObject form = new JSONObject();
        form.put("id", id);
        form.put("name", name);
        form.put("description", description);
        form.put("comment", comment);
        if (occurred != null) {
            form.put("occurred", occurred);
        }
        form.put("categories", category);
        if (indications != null) {
            form.put("indications", "");
        }
        if (modifications != null) {
            form.put("modifications", modifications);
        }
        StaplerRequest2 request = mock(StaplerRequest2.class);
        when(request.getSubmittedForm()).thenReturn(form);
        when(request.bindJSONToList(same(Indication.class), any()))
                .thenReturn((List<Indication>)indications);
        return request;
    }
}
