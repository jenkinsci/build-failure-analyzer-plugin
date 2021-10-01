/*
 * The MIT License
 *
 * Copyright 2014 Sony Mobile Communications Inc. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * A historical record of a modification occurrence by a user.
 *
 * @author Felix Hall &lt;felix.hall@sonymobile.com&gt;
 */
public class FailureCauseModification implements Serializable {
    private String user;
    private Date time;

    /**
     * Constructor for FailureCauseModification.
     *
     * @param user The user who made the modification.
     * @param time The time at which the modification was done.
     */
    @JsonCreator
    public FailureCauseModification(@JsonProperty("user") String user, @JsonProperty("time") Date time) {
        this.user = user;
        if (time == null) {
            this.time = null;
        } else {
            this.time = (Date)time.clone();
        }
    }

    /**
     * Constructor for FailureCauseModification.
     *
     * @param user The user who made the modification.
     * @param time The time at which the modification was done.
     */
    @DataBoundConstructor
    public FailureCauseModification(String user, String time) {
        this.user = user;
        if (time == null) {
            this.time = null;
        } else {
            this.time = new Date(time);
        }
    }

    /**
     * Getter for the time.
     *
     * @return The time at which the modification was done.
     */
    public Date getTime() {
        if (time == null) {
            return null;
        } else {
            return (Date)time.clone();
        }
    }

    /**
     * Getter for the user.
     *
     * @return The user who made the modification.
     */
    public String getUser() {
        return user;
    }

}
