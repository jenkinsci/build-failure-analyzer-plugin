/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications AB. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa.model.dbf;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * There is no general way to find downstream build in Jenkins Core. Different
 * plugin have there own way of keeping this information.
 * <p/>
 * Extend this class and implement
 * {@link #getDownstreamBuilds(hudson.model.AbstractBuild)}
 * in a way suitable for the plugin
 *
 * @author Jan-Olof Sivtoft
 */
public abstract class DownstreamBuildFinder implements ExtensionPoint {

    /**
     * No need to create a new empty list each time there is nothing to return.
     * Make it unmodifiable to make sure it isn't used.
     */
    protected static final List<AbstractBuild<?, ?>> EMPTY =
            Collections.unmodifiableList(new LinkedList<AbstractBuild<?, ?>>());

    /**
     * Return a list of all downstream builds originating from provided build.
     *
     * @param build get the downstream build(s) relative this build
     * @return a list with downstream builds
     */
    public abstract List<AbstractBuild<?, ?>> getDownstreamBuilds(
            final AbstractBuild build);

    /**
     * Return a list of all registered DownstreamBuildFinder of this type.
     *
     * @return a list of DownstreamBuildFinder
     */
    public static ExtensionList<DownstreamBuildFinder> getAll() {
        return Hudson.getInstance().
                getExtensionList(DownstreamBuildFinder.class);
    }
}
