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

import org.jfree.data.time.TimePeriod;

/**
 * Class containing statistics data for a time interval.
 * A time interval consists of a TimePeriod,
 * a name of the event during this period (e.g. failure cause name or failure cause category),
 * a number of times the event happened,
 * and finally an id of the event.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 */
public class FailureCauseTimeInterval {
    private TimePeriod period;
    private String name;
    private int number;
    private String id;

    /**
     * Default constructor, used when no id of the event is available.
     * For example, no id is available for failure cause categories.
     *
     * @param period the TimePeriod in which the event happened
     * @param name the name of the event
     * @param number the number of times the event happened
     */
    public FailureCauseTimeInterval(TimePeriod period, String name, int number) {
        this(period, name, null, number);
    }

    /**
     * Default constructor, used when an id of the event is available.
     *
     * @param period the TimePeriod in which the event happened
     * @param name the name of the event
     * @param id the id of the event
     * @param number the number of times the event happened
     */
    public FailureCauseTimeInterval(TimePeriod period, String name, String id, int number) {
        this.period = period;
        this.name = name;
        this.number = number;
        this.id = id;
    }

    /**
     * Getter for the time period of this interval.
     * @return time period
     */
    public TimePeriod getPeriod() {
        return period;
    }

    /**
     * Getter for the event name of this interval.
     * @return event name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the event id of this interval.
     * @return event id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the number of times the event occurred during this interval.
     * @return number of times the event occurred
     */
    public int getNumber() {
        return number;
    }

    /**
     * Adds to the number of times the event occurred.
     * @param add number to add
     */
    public void addNumber(int add) {
        number += add;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(number);
        return sb.toString();
    }

}
