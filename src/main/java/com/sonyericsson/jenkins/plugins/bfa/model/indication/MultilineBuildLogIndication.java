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
package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.MultilineBuildLogFailureReader;
import hudson.Extension;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Build log indication that matches over multiple lines.
 *
 * @author Andrew Bayer
 */
public class MultilineBuildLogIndication extends BuildLogIndication {

    /**
     * Standard constructor.
     *
     * @param pattern the string value to search for.
     */
    @DataBoundConstructor
    public MultilineBuildLogIndication(String pattern) {
        super(pattern);
    }

    /**
     * Default constructor.
     * <strong>Do not use this unless you are a serializer.</strong>
     */
    public MultilineBuildLogIndication() {
    }

    @Override
    public FailureReader getReader() {
        return new MultilineBuildLogFailureReader(this);
    }

    @Override
    public IndicationDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(MultilineBuildLogIndicationDescriptor.class);
    }

    /**
     * The descriptor.
     */
    @Extension
    public static class MultilineBuildLogIndicationDescriptor extends BuildLogIndicationDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.MultilineBuildLogIndication_DisplayName();
        }

        @Override
        protected FailureReader getFailureReader(final String testPattern) {
            return new MultilineBuildLogFailureReader(new MultilineBuildLogIndication(testPattern));
        }
    }
}
