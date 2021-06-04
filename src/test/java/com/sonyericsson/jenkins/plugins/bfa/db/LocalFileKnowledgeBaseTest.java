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
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseModification;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.util.CopyOnWriteList;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LocalFileKnowledgeBase}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginImpl.class, Jenkins.class, Metrics.class, MetricRegistry.class})
public class LocalFileKnowledgeBaseTest {

    private CopyOnWriteList<FailureCause> oldCauses;
    private FailureCause olle;
    private FailureCause existingCause;

    @Mock
    private Jenkins jenkins;
    @Mock
    private Metrics metricsPlugin;
    @Mock
    private MetricRegistry metricRegistry;

    /**
     * Some usable test data for most tests.
     */
    @Before
    public void setUp() {
        oldCauses = new CopyOnWriteList<FailureCause>();
        oldCauses.add(new FailureCause("nisse", "Nils has been in your code again!"));
        olle = new FailureCause("olle", "Olle is a good guy who wouldn't hurt a fly.");
        olle.addIndication(new BuildLogIndication(".*olle.*"));
        oldCauses.add(olle);
        existingCause = new FailureCause("existingId", "me", "I am already here!", "someComment", new Date(),
                "myCategory", new LinkedList<Indication>(), new LinkedList<FailureCauseModification>());
        oldCauses.add(existingCause);
        PluginImpl mock = PowerMockito.mock(PluginImpl.class);
        PowerMockito.mockStatic(PluginImpl.class);
        PowerMockito.when(PluginImpl.getInstance()).thenReturn(mock);

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(Metrics.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getPlugin(Metrics.class)).thenReturn(metricsPlugin);
        PowerMockito.when(metricsPlugin.metricRegistry()).thenReturn(metricRegistry);
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#LocalFileKnowledgeBase(hudson.util.CopyOnWriteList)}.
     */
    @Test
    public void testLocalFileKnowledgeBase() {

        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase(oldCauses);

        assertSame(existingCause, kb.getCause("existingId"));

        for (FailureCause c : kb.getCauses()) {
            assertNotNull(c.getName() + " should have an id", c.getId());
            if (c == olle) {
                assertFalse("olle should have an indication!", c.getIndications().isEmpty());
            }
        }
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#addCause(com.sonyericsson.jenkins.plugins.bfa.model.FailureCause)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testAddCause() throws Exception {
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
    public void testRemoveCause() throws Exception {
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
    public void testSaveCause() throws Exception {
        FailureCause expected = olle;
        expected.setId(null);
        LocalFileKnowledgeBase kb = new LocalFileKnowledgeBase();
        assertSame(expected, kb.addCause(expected));

        FailureCause toSave = new FailureCause(expected.getId(), expected.getName(),
                expected.getDescription(), expected.getComment(), expected.getLastOccurred(),
                "", expected.getIndications(), expected.getModifications());
        assertSame(toSave, kb.saveCause(toSave));
        assertNotSame(expected, kb.getCause(toSave.getId()));
    }

    /**
     * Tests {@link LocalFileKnowledgeBase#convertFrom(KnowledgeBase)}. with a LocalFileKnowledgeBase instance as the
     * old instance.
     *
     * @throws Exception if so.
     */
    @Test
    public void testConvertFromSameType() throws Exception {
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
    public void testConvertFromAnotherType() throws Exception {
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
