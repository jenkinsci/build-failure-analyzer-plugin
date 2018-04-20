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

import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Failure;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link FailureCause}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, PluginImpl.class })
public class FailureCauseTest {

    private PluginImpl pluginMock;
    private KnowledgeBase baseMock;

    /**
     * Runs before every test.
     * Mocks {@link com.sonyericsson.jenkins.plugins.bfa.PluginImpl#getInstance()} to avoid NPE's
     * when checking permissions in the code under test.
     */
    @Before
    public void setUp() {
        pluginMock = PowerMockito.mock(PluginImpl.class);
        mockStatic(PluginImpl.class);
        when(PluginImpl.getInstance()).thenReturn(pluginMock);
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
        List<String> categories = new LinkedList<String>();
        String compFail = "compilationFailure";
        String compCrashed = "computerCrashed";
        String otherError = "otherError";
        categories.add(compFail);
        categories.add(compCrashed);
        categories.add(otherError);
        when(baseMock.getCategories()).thenReturn(categories);
        FailureCause.FailureCauseDescriptor descriptor = new FailureCause.FailureCauseDescriptor();
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
     * Test for {@link FailureCause#doCheckDescription(String)} with an empty description.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescriptionEmpty() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckDescription("");
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause#doCheckDescription(String)}. With the reserved "new" description ({@link
     * CauseManagement#NEW_CAUSE_DESCRIPTION}).
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescriptionReserved() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckDescription(CauseManagement.NEW_CAUSE_DESCRIPTION);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause#doCheckDescription(String)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckDescription() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckDescription("My <b>Description</b>");
        assertSame(FormValidation.Kind.OK, validation.kind);
    }

    /**
     * Test for {@link FailureCause#doCheckName(String)} with an empty name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameEmpty() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckName("");
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause#doCheckName(String)} with the reserved "new" name ({@link
     * CauseManagement#NEW_CAUSE_NAME}).
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameReserved() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckName(CauseManagement.NEW_CAUSE_NAME);
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause#doCheckName(String)} with bad characters in the name.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckNameNoneGood() throws Exception {
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckName("[Name]");
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Test for {@link FailureCause#doCheckName(String)} when there already exists a cause with the same name.
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

        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckName("BName");
        assertSame(FormValidation.Kind.ERROR, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause#doCheckName(String)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoCheckName() throws Exception {
        LocalFileKnowledgeBase base = new LocalFileKnowledgeBase();
        when(pluginMock.getKnowledgeBase()).thenReturn(base);
        FailureCause cause = new FailureCause();
        FormValidation validation = cause.doCheckName("Some name");
        assertSame(FormValidation.Kind.OK, validation.kind);
    }

    /**
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)}. Where the intention is to save a new cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoConfigSubmitNewOk() throws Exception {
        Jenkins jenkinsMock = mock(Jenkins.class);
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkinsMock);

        mockEmptyKnowledgeBase();

        String name = "AName";
        String description = "The Description";
        String comment = "Comment";
        String category = "category";
        String pattern = ".*";
        StaplerRequest request = mockRequest("", name, description, comment, null, category,
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse response = mock(StaplerResponse.class);

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
     * Happy test for {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)}. Where the intention is to save changes to an existing cause.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoConfigSubmitSaveOk() throws Exception {
        Jenkins jenkinsMock = mock(Jenkins.class);
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkinsMock);

        mockEmptyKnowledgeBase();

        String id = "abc";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest request = mockRequest(id, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse response = mock(StaplerResponse.class);

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
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)} with a different id in the form than what the stapler binding says it is.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitSaveWrongId() throws Exception {
        Jenkins jenkinsMock = mock(Jenkins.class);
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkinsMock);

        mockEmptyKnowledgeBase();

        String origId = "abc";
        String newId = "cde";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse response = mock(StaplerResponse.class);

        FailureCause cause = new FailureCause("Old Name", "Old Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)} where the form data for id is set when it should not be since it is for a
     * new cause.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitNewWithId() throws Exception {
        Jenkins jenkinsMock = mock(Jenkins.class);
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkinsMock);

        mockEmptyKnowledgeBase();

        String origId = "";
        String newId = "cde";
        String name = "AName";
        String description = "The Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse response = mock(StaplerResponse.class);

        FailureCause cause = new FailureCause("Old Name", "Old Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Tests {@link FailureCause#doConfigSubmit(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)} where the form data for id is null when it should be for an existing
     * cause.
     *
     * @throws Exception if so
     */
    @Test(expected = Failure.class)
    public void testDoConfigSubmitNewCloneAttempt() throws Exception {
        Jenkins jenkinsMock = mock(Jenkins.class);
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkinsMock);

        mockEmptyKnowledgeBase();

        String origId = "abc";
        String newId = "";
        String name = "New Name";
        String description = "New Description";
        String comment = "New Comment";
        String pattern = ".*";
        StaplerRequest request = mockRequest(newId, name, description, comment, null, "",
                Collections.singletonList(new BuildLogIndication(pattern)), null);
        StaplerResponse response = mock(StaplerResponse.class);

        FailureCause cause = new FailureCause("A Name", "The Description");
        cause.setId(origId);
        cause.doConfigSubmit(request, response);
    }

    /**
     * Helper to return instance of {@link FailureCause}
     * @param name name of the cause
     * @param date instance of {@link Date}
     * @param i list of {@link Indication}
     * @return {@link FailureCause}
     */
    public FailureCause getCauseForEquality(String name, Date date, List<Indication> i) {
        return new FailureCause(name, "myFailureCause", "description", "comment", date,
                "category", i, null);
    }

    /**
     * Test two {@link FailureCause} instances are equal
     */
    @Test
    public void testEquals() {
        Date d = new Date();
        Indication i = new BuildLogIndication("something");
        FailureCause cause1 = getCauseForEquality("foo", d, Collections.singletonList(i));
        FailureCause cause2 = getCauseForEquality("foo", d, Collections.singletonList(i));
        assertEquals(cause1, cause2);
    }

    /**
     * Test two {@link FailureCause} instances are not equal
     */
    @Test
    public void testNotEquals(){
        Date d = new Date();
        Indication i = new BuildLogIndication("something");
        FailureCause cause1 = getCauseForEquality("foo1", d, Collections.singletonList(i));
        FailureCause cause2 = getCauseForEquality("foo2", d, Collections.singletonList(i));
        assertNotEquals(cause1, cause2);
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
     *                      {@link StaplerRequest#bindJSONToList(Class, Object)}
     * @param modifications the modification history of this FailureCause
     * @return a mocked request object.
     *
     * @throws ServletException if so, but probably not.
     */
    private StaplerRequest mockRequest(String id, String name, String description, String comment, Date occurred,
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
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getSubmittedForm()).thenReturn(form);
        when(request.bindJSONToList(same(Indication.class), Matchers.<Object>anyObject()))
                .thenReturn((List<Indication>)indications);
        return request;
    }
}
