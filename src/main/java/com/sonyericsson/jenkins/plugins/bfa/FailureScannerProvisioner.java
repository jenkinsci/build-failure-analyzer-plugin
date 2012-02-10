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

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A RunListener that puts the {@link FailureScanner} into all builds.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension
public class FailureScannerProvisioner extends RunListener<AbstractBuild> {

    private static final Logger logger = Logger.getLogger(FailureScannerProvisioner.class.getName());

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        if (build.getProject().getPublishersList().get(FailureScanner.class) == null) {
            try {
                build.getProject().getPublishersList().add(new FailureScanner());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to add a FailureScanner to "
                        + build.getProject().getFullDisplayName(), e);
                listener.getLogger().println("[BFA] WARNING! Failed to add the scanner to this job, "
                        + "failure analysis will not be performed.");
            }
        }
    }
}
