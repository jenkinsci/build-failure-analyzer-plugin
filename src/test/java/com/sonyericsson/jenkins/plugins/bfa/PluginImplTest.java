/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa;

import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * JUnit 4 Tests for {@link PluginImpl}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImplTest {

    private MockedStatic<Jenkins> jenkinsMockedStatic;

    /**
     * Initial mocking.
     */
    @Before
    public void setUp() {
        Jenkins jenkins = mock(Jenkins.class);
        jenkinsMockedStatic = mockStatic(Jenkins.class);
        jenkinsMockedStatic.when(Jenkins::getInstance).thenReturn(jenkins);
    }

    /**
     * Release all the static mocks.
     */
    @After
    public void tearDown() {
        jenkinsMockedStatic.close();
    }

    /**
     * Tests {@link PluginImpl#getStaticResourcesBase()}. Just a simple one.
     * <p/>
     * TODO Make a similar test in a full Jenkins context.
     *
     * @throws Exception if so.
     */
    @Test
    public void testGetStaticResourcesBase() throws Exception {
        assertEquals("/plugin/build-failure-analyzer", PluginImpl.getStaticResourcesBase());
    }


    /**
     * Tests {@link PluginImpl#getStaticImagesBase()}}. Just a simple one.
     * <p/>
     * TODO Make a similar test in a full Jenkins context.
     *
     * @throws Exception if so.
     */
    @Test
    public void testGetStaticImagesBase() throws Exception {
        assertEquals("/plugin/build-failure-analyzer/images", PluginImpl.getStaticImagesBase());
    }

    /**
     * Tests that you can't set {@link PluginImpl#setNrOfScanThreads(int)} to 0.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetNrOfScanThreadsZero() {
        PluginImpl plugin = new PluginImpl();
        plugin.setNrOfScanThreads(0);
    }
}
