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
package com.sonyericsson.jenkins.plugins.bfa;

import hudson.Functions;
import java.io.Serializable;

/**
 * Helper for annotating the lines, creates the strings to annotate with.
 *
 * @author Tomas Westling&lt;tomas.westling@sonymobile.com&gt;
 */
public class AnnotationHelper implements Serializable {

    private String before = "";
    private String after = "";
    private String title = "";

    /**
     * Gets the String to annotate with before the console text.
     *
     * @return the String to put before the console text.
     */
    public String getBefore() {
        if (!title.isEmpty()) {
            return before
                    + "<span style=\"color:white;background:red\" title=\""
                    + Functions.htmlAttributeEscape(title) + "\">";
        } else {
            return before;
        }
    }

    /**
     * Gets the String to annotate with after the console text.
     *
     * @return the String to put after the console text.
     */
    public String getAfter() {
        return after;
    }

    /**
     * Adds a focus id line before the console text.
     *
     * @param id the id for the line, to refer to in links.
     */
    public void addFocus(String id) {
        // This style should shift the anchor down below the header so that it's visible.
        // "&nbsp;" may be required to make this work for WebKit-based browsers.
        this.before += "<a id=\"" + id + "\" style=\""
            + "display:block;position:relative;top:-2em;visibility:hidden"
            + "\">&nbsp;</a>";
    }

    /**
     * Adds a title for the line.
     *
     * @param addedTitle the title of the line.
     */
    public void addTitle(String addedTitle) {
        if (this.title.equals("")) {
            this.title = addedTitle;
        } else {
            this.title += "\n" + addedTitle;
        }
    }

    /**
     * Adds a String to annotate with, after the console text.
     *
     * @param addedAfter the String to put after the console text.
     */
    public void addAfter(String addedAfter) {
        this.after += addedAfter;
    }
}
