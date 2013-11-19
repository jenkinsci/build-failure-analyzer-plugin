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

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

/**
 * Utility class.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public final class BfaUtils {

    private static final Logger logger = Logger.getLogger(BfaUtils.class.getName());

    /**
     * Utility classes should not have a public or default constructor.
     */
    private BfaUtils() {
    }

    /**
     * Gets the Jenkins master name.
     * @return the master name
     */
    public static String getMasterName() {
        String masterString = Jenkins.getInstance().getRootUrl();
        String name = null;

        if (masterString == null) {
            logger.log(Level.WARNING, "Couldn't get name of master: Jenkins root url is null");
        } else {
            try {
                name = new URL(masterString).getHost();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't get name of master: ", e);
            }
        }
        return name;
    }

}
