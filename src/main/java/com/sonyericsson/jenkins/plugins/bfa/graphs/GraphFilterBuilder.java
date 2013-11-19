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
package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.Date;
import java.util.List;

/**
 * Class for filtering statistics data, used when building graphs.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public class GraphFilterBuilder {

    private String masterName;
    private String slaveName;
    private String projectName;
    private List<Integer> buildNumbers;
    private Date since;
    private String result;
    private String excludeResult;

    /**
     * Adds a filter on master name, which only allows items that have this name.
     * @param masterName master name to allow
     */
    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    /**
     * Adds a filter on slave name, which only allows items that have this name.
     * @param slaveName project name to allow
     */
    public void setSlaveName(String slaveName) {
        this.slaveName = slaveName;
    }

    /**
     * Adds a filter on project name, which only allows items that have this name.
     * @param projectName project name to allow
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Adds a filter on build numbers, which only allows items that have
     * one of the numbers in the argument list.
     * @param buildNumbers build numbers to allow
     */
    public void setBuildNumbers(List<Integer> buildNumbers) {
        this.buildNumbers = buildNumbers;
    }

    /**
     * Adds a filter on date, which only allows items with date later than
     * the argument date.
     * @param since first date to allow
     */
    public void setSince(Date since) {
        if (since != null) {
            this.since = new Date(since.getTime());
        }
    }

    /**
     * Adds a filter on build result, which only allows items with this result.
     * @param result result to allow
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * Adds a filter on build result, which excludes items with this result.
     * @param excludeResult result to exclude
     */
    public void setExcludeResult(String excludeResult) {
        this.excludeResult = excludeResult;
    }

    /**
     * Getter for filter on master name.
     * @return the master name that is allowed
     */
    public String getMasterName() {
        return masterName;
    }

    /**
     * Getter for filter on slave name.
     * @return the slave name that is allowed
     */
    public String getSlaveName() {
        return slaveName;
    }

    /**
     * Getter for filter on project name.
     * @return the project name that is allowed
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Getter for filter on build numbers.
     * @return list of allowed build numbers
     */
    public List<Integer> getBuildNumbers() {
        return buildNumbers;
    }

    /**
     * Getter for filter on date.
     * @return first date that is allowed
     */
    public Date getSince() {
        if (since == null) {
            return null;
        }
        return new Date(since.getTime());
    }

    /**
     * Getter for filter on build result.
     * @return result that is allowed
     */
    public String getResult() {
        return result;
    }

    /**
     * Getter for filter on build result.
     * @return result that is excluded
     */
    public String getExcludeResult() {
        return excludeResult;
    }

}
