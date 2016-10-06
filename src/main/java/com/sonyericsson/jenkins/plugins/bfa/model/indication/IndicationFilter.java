/*
 * The MIT License
 *
 * Copyright 2016 Axis Communications AB. All rights reserved.
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
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Allows for blocking/filtering Indications from users. This could be used
 * to hide the included core Indicators, when plug-in provided Indicators
 * provide a superset (or the same) of the now hidden Indicators features.
 */
public abstract class IndicationFilter implements ExtensionPoint {
    /**
     * List of blocked indicator descriptors.
     *
     * @return The descriptors of the Indicators intended to be hidden from the caller
     */
    public abstract List<Indication.IndicationDescriptor> getBlockedIndicators();

    /**
     * Fetch all IndicationFilters.
     *
     * @return list of all IndicationFilters
     */
    public static ExtensionList<IndicationFilter> all() {
        return Jenkins.getInstance().getExtensionList(IndicationFilter.class);
    }
}
