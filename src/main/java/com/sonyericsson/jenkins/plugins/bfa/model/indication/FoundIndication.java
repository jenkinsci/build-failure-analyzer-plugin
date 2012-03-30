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

import hudson.MarkupText;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * The number of lines to show above the found Indication
     */
    private static final int CONTEXT = 10;
    private static final Logger logger = Logger.getLogger(FoundIndication.class.getName());

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

    /**
     * Adds extra information to the log and presents it.
     *
     * @return the log file of this indication, with extra information.
     */
    public String getModifiedLog() {
        StringBuilder builder = new StringBuilder("<pre>");
        String currentLine;
        int currentLineNumber = 1;
        int focusLine;
        if (matchingLine < CONTEXT) {
            focusLine = 1;
        } else {
            focusLine = matchingLine - CONTEXT;
        }
        File rootDir = build.getRootDir();
        File inputFile = new File(rootDir, matchingFile);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), FILE_ENCODING));
            while ((currentLine = br.readLine()) != null) {
                MarkupText markup = new MarkupText(currentLine);
                currentLine = markup.toString(true);
                if (currentLineNumber == focusLine) {
                    //if focusLine and matchingLine both are equal to the first line.
                    if (currentLineNumber == matchingLine) {
                        builder.append("<span class=\"errorLine\" id=\"focusLine\">");
                        builder.append(currentLine);
                        builder.append("</span>\n");
                    } else {
                        builder.append("<span id=\"focusLine\">");
                        builder.append(currentLine);
                        builder.append("</span>\n");
                    }
                } else if (currentLineNumber == matchingLine) {
                    builder.append("<span class=\"errorLine\">");
                    builder.append(currentLine);
                    builder.append("</span>\n");
                } else if (currentLineNumber != focusLine && currentLineNumber != matchingLine) {
                    builder.append(currentLine);
                    builder.append("\n");
                }
                currentLineNumber++;
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "[BFA] Could not open reader for build: " + build.getDisplayName()
                    + " and Indication: " + pattern, e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[BFA] I/O problems during build log modification for build:"
                    + build.getDisplayName() + " and Indication: " + pattern, e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close the reader. ", e);
                }
            }
        }
        builder.append("</pre>");
        return builder.toString();
    }
}
