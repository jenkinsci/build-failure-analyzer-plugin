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

package com.sonyericsson.jenkins.plugins.bfa.model;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.model.AbstractBuild;
import org.codehaus.jackson.annotate.JsonIgnoreType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

/**
 * Reader used to find indications of a failure cause.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@JsonIgnoreType
public abstract class FailureReader {

    /** The indication we are looking for. */
    protected Indication indication;

    /**
     * Standard constructor.
     * @param indication the indication to look for.
     */
    public FailureReader(Indication indication) {
        this.indication = indication;
    }

    /**
     * Scans for indications of a failure cause.
     * @param build the build to scan for indications.
     * @param buildLog the log of the build.
     * @return a FoundIndication if something was found, null if not.
     */
    public abstract FoundIndication scan(AbstractBuild build, PrintStream buildLog);

    /**
     * Scans one file for the required pattern.
     * @param build the build we are processing.
     * @param reader the reader to read from.
     * @param currentFile the file path of the file we want to scan.
     * @return a FoundIndication if we find the pattern, null if not.
     * @throws IOException if problems occur in the reader handling.
     */
    protected FoundIndication scanOneFile(AbstractBuild build, BufferedReader reader, String currentFile)
            throws IOException {
        FoundIndication foundIndication = null;
        boolean found = false;
        Pattern pattern = indication.getPattern();
        String line;
        int currentLine = 1;
        while ((line = reader.readLine()) != null) {
            if (pattern.matcher(line).find()) {
                found = true;
                break;
            }
            currentLine++;
        }
        if (found) {
            foundIndication = new FoundIndication(build, pattern.pattern(), currentFile, currentLine);
        }
        return foundIndication;
    }
}
