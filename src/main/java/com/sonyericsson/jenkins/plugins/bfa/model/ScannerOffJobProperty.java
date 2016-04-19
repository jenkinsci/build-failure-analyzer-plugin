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

package com.sonyericsson.jenkins.plugins.bfa.model;

import hudson.model.AbstractProject;
import hudson.model.JobProperty;

import java.io.Serializable;

/**
 * A JobProperty that flags a job that should not be scanned.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 * @deprecated {@link ScannerJobProperty} is used instead, but this is kept to be able to de-serialize old jobs.
 */
@Deprecated
public class ScannerOffJobProperty extends JobProperty<AbstractProject<?, ?>> implements Serializable {
    private boolean doNotScan;

    /**
     * Standard Constructor.
     *
     * @param doNotScan signal that builds of this job should not be scanned.
     */
    public ScannerOffJobProperty(boolean doNotScan) {
        this.doNotScan = doNotScan;
    }

    /**
     * Default Constructor. <strong>Do not use unless you are a serializer!</strong>
     */
    public ScannerOffJobProperty() {
    }

    /**
     * The value. True turns the scanner off.
     *
     * @return if no scan should be done.
     */
    public boolean isDoNotScan() {
        return doNotScan;
    }

    /**
     * De-serialize this object to a {@link ScannerJobProperty}.
     *
     * @return an instance of {@link ScannerJobProperty} with the same data.
     */
    public Object readResolve() {
        return new ScannerJobProperty(doNotScan);
    }
}
