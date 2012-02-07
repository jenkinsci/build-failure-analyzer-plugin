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
package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import hudson.ExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Reader;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Indication that can match a search string for a specific reader.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public abstract class Indication implements Describable<Indication>, Serializable {

    private Pattern pattern;

    /**
     * @param pattern the String value.
     */
    @DataBoundConstructor
    public Indication(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    /**
     * Default constructor. <strong>Do not use this unless you are a serializer.</strong>
     */
    protected Indication() {
    }

    /**
     * Gets the reader for the specific build.
     *
     * @param build the build to analyze.
     * @return a reader that can we will use to look for a pattern.
     *
     * @throws Exception if the reader could not be loaded.
     */
    public abstract Reader getReader(AbstractBuild build) throws Exception;

    /**
     * Getter for the pattern to match.
     *
     * @return the pattern to match.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * The descriptor for this indicator.
     */
    public abstract static class IndicationDescriptor extends Descriptor<Indication> {

        /**
         * Provides a list of all registered descriptors of this type.
         *
         * @return the list of descriptors.
         */
        public static ExtensionList<IndicationDescriptor> getAll() {
            return Hudson.getInstance().getExtensionList(IndicationDescriptor.class);
        }
    }

}
