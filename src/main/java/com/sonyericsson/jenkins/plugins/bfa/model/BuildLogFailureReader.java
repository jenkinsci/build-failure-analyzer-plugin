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

import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Reader used to find indications of a failure cause.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class BuildLogFailureReader extends FailureReader {

    private static final Logger logger = Logger.getLogger(BuildLogFailureReader.class.getName());

    /**
     * Standard constructor.
     * @param indication the indication to look for.
     */
    public BuildLogFailureReader(BuildLogIndication indication) {
        super(indication);
    }

    @Override
    public FoundIndication scan(AbstractBuild build, PrintStream buildLog) {
        FoundIndication foundIndication = null;
        String currentFile = build.getLogFile().getName();
        BufferedReader reader = null;
        long start = System.currentTimeMillis();
        try {
            reader = new BufferedReader(build.getLogReader());
            foundIndication = scanOneFile(build, reader, currentFile);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "[BFA] I/O problems during indication analysis: ", ioe);
            buildLog.println("[BFA] I/O problems during indication analysis.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[BFA] Could not open reader for indication: ", e);
            buildLog.println("[BFA] Could not open reader for indication.");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close the reader. ", e);
                }
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "[BFA] [{0}] - [{1}] {2}ms",
                        new Object[]{build.getFullDisplayName(),
                                     indication.toString(),
                                     String.valueOf(System.currentTimeMillis()-start)});
            }
        }
        return foundIndication;
    }
}
