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
package com.sonyericsson.jenkins.plugins.bfa.utils;

import java.util.Comparator;

/**
 * Class for keeping track of the number of copies of the same object.
 * Can for example be used for keeping track of how many times
 * a specific FailureCause has been triggered.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonyericsson.com&gt;
 *
 * @param <O> the object type that we count
 */
public class ObjectCountPair<O> {

    private O object;
    private int count;

    /**
     * Standard constructor.
     * @param object the object we are counting
     * @param count the number of copies we have of the object
     */
    public ObjectCountPair(O object, int count) {
        this.object = object;
        this.count = count;
    }

    /**
     * Getter for the object we are counting.
     * @return the object we are counting
     */
    public O getObject() {
        return object;
    }

    /**
     * Getter for the counter.
     * @return the number of copies of the object
     */
    public int getCount() {
        return count;
    }

    /**
     * Adds argument number to the counter.
     * @param add integer value to add
     */
    public void addCount(int add) {
        count += add;
    }

    /**
     * Gets a comparator that compares the count.
     * @return comparator
     */
    public static Comparator<ObjectCountPair> countComparator() {
        return new Comparator<ObjectCountPair>() {
            @Override
            public int compare(ObjectCountPair o1, ObjectCountPair o2) {
                return o2.getCount() - o1.getCount();
            }
        };
    }

}
