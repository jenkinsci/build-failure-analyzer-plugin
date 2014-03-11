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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import org.codehaus.jackson.annotate.JsonIgnoreType;

/**
 * Reader used to find indications of a failure cause.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@JsonIgnoreType
public abstract class FailureReader {

    private static final Logger logger = Logger.getLogger(FailureReader.class.getName());

    private static final long TIMEOUT_FILE = 10000;
    private static final long TIMEOUT_LINE = 1000;
    private static final long SLEEPTIME = 200;

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
        TimerThread timerThread = new TimerThread(Thread.currentThread(), TIMEOUT_LINE);
        FoundIndication foundIndication = null;
        boolean found = false;
        Pattern pattern = Pattern.compile("^[\r\n]*?" + indication.getPattern().pattern() + "[^\r\n]*?$",
                Pattern.MULTILINE);
        Scanner scanner = new Scanner(reader);
        scanner.useDelimiter(Pattern.compile("[\\r\\n]+"));
        String firstLine = "";
        timerThread.start();

        try {
            long startTime = System.currentTimeMillis();

            while (scanner.hasNext()) {
                try {
                    String lines = scanner.findWithinHorizon(pattern, 10000);
                    if (lines != null) {
                        StringTokenizer tokenizer = new StringTokenizer(lines);
                        firstLine = tokenizer.nextToken("\n\r\f");
                        found = true;
                        break;
                    }
                    scanner.next();
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        logger.warning("Timeout scanning for indication '" + indication.toString() + "' for file "
                                + currentFile);
                    } else {
                        // This is not a timeout exception
                        throw e;
                    }
                }
                timerThread.touch();
                if (System.currentTimeMillis() - startTime > TIMEOUT_FILE) {
                    logger.warning("File timeout scanning for indication '" + indication.toString() + "' for file "
                            + currentFile);
                    break;
                }
            }

            if (found) {
                String cleanLine = ConsoleNote.removeNotes(firstLine);
                foundIndication = new FoundIndication(build, pattern.toString(), currentFile, cleanLine);
            }

            return foundIndication;
        } finally {
            timerThread.requestStop();
            timerThread.interrupt();
            try {
                timerThread.join();
                //CS IGNORE EmptyBlock FOR NEXT 2 LINES. REASON: unimportant exception
            } catch (InterruptedException eIgnore) {
            }
            // reset the interrupt
            Thread.interrupted();
        }
    }

    /**
     * CharSequence that notices thread interrupts -- as might be necessary
     * to recover from a loose regex on unexpected challenging input.
     */
    public static class InterruptibleCharSequence implements CharSequence {
        CharSequence inner;

        /**
        * Standard constructor.
        * @param inner the CharSequence to be able to interrupt.
        */
        public InterruptibleCharSequence(CharSequence inner) {
            super();
            this.inner = inner.toString();
        }

        @Override
        public char charAt(int index) {
            if (Thread.interrupted()) { // clears flag if set
                throw new RuntimeException(new InterruptedException());
            }
            return inner.charAt(index);
        }

        @Override
        public int length() {
            return inner.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new InterruptibleCharSequence(inner.subSequence(start, end));
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }

    /**
     * TimerThread interrupting a monitored thread unless TimerThread is touched within the
     * specified timeout value
     */
    static class TimerThread extends Thread {

        private Thread monitorThread;
        private boolean stop = false;
        private long timeout;
        private long lastTouched;

        /**
       * Standard constructor.
       * @param monitorThread The thread to monitor and interrupt after the timeout.
       * @param timeout The timeout in ms.
         */
        TimerThread(Thread monitorThread, long timeout) {
            this.monitorThread = monitorThread;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            lastTouched = System.currentTimeMillis();
            while (!stop) {
                try {
                    Thread.sleep(SLEEPTIME);
                    if (System.currentTimeMillis() - lastTouched >= timeout) {
                        monitorThread.interrupt();
                        // timeout met, interrupt the launcherThread
                    }
                //CS IGNORE EmptyBlock FOR NEXT 5 LINES. REASON: timeout exception
                } catch (InterruptedException eRestartSleep) {
                    // My thread was interrupted so continue loop and
                    // check if I'm stopped and otherwise just restart sleep
                }
            }
        }

        /**
        * Touch, i.e. reset countdown timer.
        */
        public void touch() {
            lastTouched = System.currentTimeMillis();
        }

        /**
         * Set stop flag to stop executing
         */
        public void requestStop() {
            stop = true;
        }
    }
}
