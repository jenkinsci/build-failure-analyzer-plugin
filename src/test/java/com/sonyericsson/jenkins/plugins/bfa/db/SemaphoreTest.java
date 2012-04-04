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

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static junit.framework.Assert.assertEquals;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for the Semaphore.
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class SemaphoreTest {

    /**
     * Tests that acquire and release works in the the correct way for the Semaphore.
     * @throws Exception if so.
     */
    @Test(timeout = 20000)
    public void testAcquireAndRelease() throws Exception {
        Semaphore semaphore = new Semaphore();
        java.util.concurrent.Semaphore innerSemaphore = Whitebox.getInternalState(semaphore, "semaphore");
        assertEquals("The semaphore should have no available permits to start with", 0,
                innerSemaphore.availablePermits());
        semaphore.release();
        semaphore.release();
        assertEquals("The semaphore should have 2 available permits", 2, innerSemaphore.availablePermits());
        semaphore.acquire();
        assertEquals("All of the semaphore's permits should be taken", 0, innerSemaphore.availablePermits());
    }
}
