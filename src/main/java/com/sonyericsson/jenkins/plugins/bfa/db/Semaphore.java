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

/**
 * "Real" binary semaphore, where subsequent calls to release do not
 * make the semaphore be able to acquire more than the maximum permits.
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class Semaphore {

    private java.util.concurrent.Semaphore semaphore;

    /**
     * Standard constructor.
     */
    public Semaphore() {
        semaphore = new java.util.concurrent.Semaphore(0);
    }

    /**
     * @see java.util.concurrent.Semaphore#acquire()
     * The difference is that this tries to acquire all available permits if there are any and 1 if there are none.
     * @throws InterruptedException if the java.util.concurrent.Semaphore is interrupted.
     */
    public void acquire() throws InterruptedException {
        int take;
        if (semaphore.availablePermits() < 1) {
            take = 1;
        } else {
            take = semaphore.availablePermits();
        }
        semaphore.acquire(take);
    }

    /**
     * @see java.util.concurrent.Semaphore#release()
     * Releases a permit, returning it to the semaphore.
     */
    public void release() {
        semaphore.release();
    }
}
