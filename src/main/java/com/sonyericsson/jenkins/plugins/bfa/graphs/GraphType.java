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

import java.util.HashMap;
import java.util.Map;

/**
 * Enum for graph types.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public enum GraphType {

    /**
     * Small bar chart.
     */
    BAR_CHART_CAUSES_SMALL(1),

    /**
     * Constant for bar chart with {@link FailureCause}s.
     */
    BAR_CHART_CAUSES(2),

    /**
     * Constant for bar chart with categories.
     */
    BAR_CHART_CATEGORIES(3),

    /**
     * Constant for bar chart with build numbers.
     */
    BAR_CHART_BUILD_NBRS(4),

    /**
     * Constant for pie chart with {@link FailureCause}s.
     */
    PIE_CHART_CAUSES(5),

    /**
     * Constant for pie chart with categories.
     */
    PIE_CHART_CATEGORIES(6),

    /**
     * Constant for time series chart with {@link FailureCause}s.
     */
    TIME_SERIES_CHART_CAUSES(7),

    /**
     * Constant for time series chart with categories.
     */
    TIME_SERIES_CHART_CATEGORIES(8),

    /**
     * Constant for time series chart displaying unknown failure causes.
     */
    TIME_SERIES_UNKNOWN_FAILURES(9);

    private int value;
    private static Map<Integer, GraphType> typesByValue = new HashMap<Integer, GraphType>();

    /**
     * Standard constructor.
     * @param value integer representation of enum
     */
    private GraphType(int value) {
        this.value = value;
    }

    /**
     * Gets the integer representation of this enum.
     * @return integer representation
     */
    public int getValue() {
        return value;
    }

    /**
     * Maps an integer value to an enum.
     * @param value to map
     * @return matching enum type
     */
    public static GraphType toEnum(int value) {
        return typesByValue.get(value);
    }

    static {
        for (GraphType type : GraphType.values()) {
            typesByValue.put(type.value, type);
        }
    }

}
