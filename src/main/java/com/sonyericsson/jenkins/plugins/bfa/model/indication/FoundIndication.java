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

package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import hudson.model.AbstractBuild;

/**
 * Found Indication of an unsuccessful build.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class FoundIndication {

    /**
     * The platform file encoding. We assume that Jenkins uses it when writing the logs.
     */
    protected static final String FILE_ENCODING = System.getProperty("file.encoding");
    private String matchingFile;
    private int matchingLine;
    private String pattern;
    private AbstractBuild build;

    /**
     * Standard constructor.
     *
     * @param build           the build of this indication.
     * @param originalPattern the original pattern we used to match.
     * @param matchingFile    the path to the file in which we found the match.
     * @param matchingLine    the line on which we found the match.
     */
    public FoundIndication(AbstractBuild build, String originalPattern, String matchingFile, int matchingLine) {
        this.pattern = originalPattern;
        this.matchingFile = matchingFile;
        this.matchingLine = matchingLine;
        this.build = build;
    }

    /**
     * Getter for the matching file.
     *
     * @return the file in which we found the match.
     */
    public String getMatchingFile() {
        return matchingFile;
    }

    /**
     * Getter for the matching line.
     *
     * @return the line on which we found the match.
     */
    public int getMatchingLine() {
        return matchingLine;
    }

    /**
     * Getter for the pattern.
     *
     * @return the pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Getter for the build.
     *
     * @return the build.
     */
    public AbstractBuild getBuild() {
        return build;
    }
}
