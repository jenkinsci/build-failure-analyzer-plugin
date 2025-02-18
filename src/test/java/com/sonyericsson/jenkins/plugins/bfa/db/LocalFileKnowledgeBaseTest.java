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
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import hudson.util.CopyOnWriteList;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalFileKnowledgeBase}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
class LocalFileKnowledgeBaseTest {

    private CopyOnWriteList<FailureCause> oldCauses;
    private FailureCause olle;
    private FailureCause existingCause;

    private Jenkins jenkins;
    private Metrics metricsPlugin;
    private MetricRegistry metricRegistry;
    private MockedStatic<PluginImpl> pluginMockedStatic;
    private MockedStatic<Jenkins> jenkinsMockedStatic;
    private MockedStatic<Metrics> metricsMockedStatic;

    /**
     * Some usable test data for most tests.
     */
    @BeforeEach
    void setUp() {
        jenkins = mock(Jenkins.class);
        metricsPlugin = mock(Metrics.class);
        metricRegistry = mock(MetricRegistry.class);
        oldCauses = new CopyOnWriteList<>();
        oldCauses.add(new FailureCause("nisse", "Nils has been in your code again!"));
        olle = new FailureCause("olle", "Olle is a good guy who wouldn't hurt a fly.");
        olle.addIndication(new BuildLogIndication(".*olle.*"));
        oldCauses.add(olle);
        existingCause = new FailureCause("existingId", "me", "I am already here!", "someComment", new Date(),
                "myCategory", new LinkedList<>(), new LinkedList<>());
        oldCauses.add(existingCause);
        PluginImpl mock = mock(PluginImpl.class);
        pluginMockedStatic = mockStatic(PluginImpl.class);
        pluginMockedStatic.when(PluginImpl::getInstance).thenReturn(mock);

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
        pluginMockedStatic.close();
        jenkinsMockedStatic.close();
        metricsMockedStatic.close();
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#LocalFileKnowledgeBase(hudson.util.CopyOnWriteList)}.
     */
    @Test
    void testLocalFileKnowledgeBase() {

        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase(oldCauses);

        assertSame(existingCause, kb.getCause("existingId"));

        for (FailureCause c : kb.getCauses()) {
            assertNotNull(c.getId(), c.getName() + " should have an id");
            if (c == olle) {
                assertFalse(c.getIndications().isEmpty(), "olle should have an indication!");
            }
        }
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#addCause(com.sonyericsson.jenkins.plugins.bfa.model.FailureCause)}.
     *
     * @throws Exception if so.
     */
    @Test
    void testAddCause() throws Exception {
        FailureCause expected = new FailureCause("olle", "description");
        expected.addIndication(new BuildLogIndication(".*"));
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        assertSame(expected, kb.addCause(expected));
        assertNotNull(expected.getId());
        assertFalse(expected.getId().isEmpty());
        assertSame(expected, kb.getCause(expected.getId()));

    }

    /**
     * Tests {@link LocalFileKnowledgeBase#removeCause(String)}.
     *
     * @throws Exception if so.
     */
    @Test
    void testRemoveCause() throws Exception {
        FailureCause expected = new FailureCause("olle", "description");
        expected.addIndication(new BuildLogIndication(".*"));
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        FailureCause addedCause = kb.addCause(expected);

        assertSame(addedCause, kb.removeCause(addedCause.getId()));
        assertNull(kb.getCause(addedCause.getId()));
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#saveCause(com.sonyericsson.jenkins.plugins.bfa.model.FailureCause)}.
     *
     * @throws Exception if so.
     */
    @Test
    void testSaveCause() throws Exception {
        FailureCause expected = olle;
        expected.setId(null);
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        assertSame(expected, kb.addCause(expected));

        FailureCause toSave = new FailureCause(expected.getId(), expected.getName(),
                expected.getDescription(), expected.getComment(), expected.getLastOccurred(),
                "", expected.getIndications(), expected.getModifications());
        assertSame(toSave, kb.saveCause(toSave));
        assertNotSame(expected, kb.getCause(toSave.getId()));
        verify(metricRegistry, times(1)).counter(Mockito.anyString());
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#convertFrom(KnowledgeBase)}. with a LocalFileKnowledgeBase instance as the
     * old instance.
     *
     * @throws Exception if so.
     */
    @Test
    void testConvertFromSameType() throws Exception {
        LocalFileKnowledgeBase old = new LocalFileKnowledgeBase(oldCauses);
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        kb.convertFrom(old);
        assertTrue(kb.getCauses().contains(olle));
        assertSame(existingCause, kb.getCause(existingCause.getId()));
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#convertFrom(KnowledgeBase)}.
     *
     * @throws Exception if so.
     */
    @Test
    void testConvertFromAnotherType() throws Exception {
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        String zeroId = UUID.randomUUID().toString();
        oldCauses.get(0).setId(zeroId);
        String oneId = UUID.randomUUID().toString();
        oldCauses.get(1).setId(oneId);
        KnowledgeBase old = mock(KnowledgeBase.class);
        when(old.getCauses()).thenReturn(oldCauses.getView());
        when(old.getCauseNames()).thenReturn(oldCauses.getView());
        when(old.getCause(eq(zeroId))).thenReturn(oldCauses.get(0));
        when(old.getCause(eq(oneId))).thenReturn(oldCauses.get(1));
        when(old.getCause(eq(existingCause.getId()))).thenReturn(existingCause);

        kb.convertFrom(old);

        assertSame(existingCause, kb.getCause(existingCause.getId()));
    }
}
