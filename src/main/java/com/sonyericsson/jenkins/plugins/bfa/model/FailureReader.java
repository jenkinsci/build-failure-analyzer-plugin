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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.google.common.base.Joiner;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.Util;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader used to find indications of a failure cause.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@JsonIgnoreType
public abstract class FailureReader {

    private static final Logger logger = Logger.getLogger(FailureReader.class.getName());

    private static final long TIMEOUT_BLOCK = 2000;
    private static final long TIMEOUT_FILE = 10000;
    private static final long TIMEOUT_LINE = 1000;
    private static final long SLEEPTIME = 200;

    /**
     * Overlapping bytes when moving the sliding window searching area.
     * A value of 5000 essentially means that a regular expression can span
     * 5000 bytes (~50 lines) anywhere in the buildlog and still get a match.
     * Used when scanning for
     * {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication}.
     *
     * Can never be larger than BUF_SIZE_BYTES.
     */
    private static final int OVERLAP_BYTES = 5000;

    /**
     * The read buffer size for scanMultiLineOneFile(). This is also the size
     * of the total "search area" when moving the sliding window through
     * the buildlog. Used when scanning for
     * {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication}.
     */
    private static final int BUF_SIZE_BYTES = 15000;

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
     * Scans a build log.
     *
     * @param build - the build whose log should be scanned.
     * @return a FoundIndication if the pattern given by this FailureReader
     * is found in the log of the given build; return null otherwise.
     * @throws IOException if so.
     * @deprecated use {@link #scan(hudson.model.Run)}.
     */
    @Deprecated
    public FoundIndication scan(AbstractBuild build) throws IOException {
        if (Util.isOverridden(FailureReader.class, getClass(), "scan", Run.class)) {
            return scan((Run)build);
        }
        return null;
    }

    /**
     * Scans a build log.
     *
     * @param build - the build whose log should be scanned.
     * @return a FoundIndication if the pattern given by this FailureReader
     * is found in the log of the given build; return null otherwise.
     * @throws IOException if so.
     */
    public FoundIndication scan(Run build) throws IOException {
        if (Util.isOverridden(FailureReader.class, getClass(), "scan", AbstractBuild.class)) {
            return scan((AbstractBuild)build);
        }
        return null;
    }

    /**
     * Scans for indications of a failure cause.
     * @param build the build to scan for indications.
     * @param buildLog the log of the build.
     * @return a FoundIndication if something was found, null if not.
     * @deprecated Use {@link #scan(hudson.model.Run, java.io.PrintStream)}.
     */
    @Deprecated
    public FoundIndication scan(AbstractBuild build, PrintStream buildLog) {
        if (Util.isOverridden(FailureReader.class, getClass(), "scan", Run.class, PrintStream.class)) {
            return scan((Run)build, buildLog);
        }
        return null;
    }

    /**
     * Scans for indications of a failure cause.
     * @param build the build to scan for indications.
     * @param buildLog the log of the build.
     * @return a FoundIndication if something was found, null if not.
     */
    public FoundIndication scan(Run build, PrintStream buildLog) {
        if (Util.isOverridden(FailureReader.class, getClass(), "scan", AbstractBuild.class, PrintStream.class)) {
            return scan((AbstractBuild)build, buildLog);
        }
        return null;
    }

    /**
     * Checks all patterns one-by-one for entire file.
     *
     * @param causes list of failure causes that we a looking for.
     * @param build current build.
     * @param reader file reader.
     * @param currentFile file name.
     * @return found indications.
     * @throws IOException Exception.
     */
   public static List<FoundFailureCause> scanSingleLinePatterns(List<FailureCause> causes,
                                                              Run build,
                                                              BufferedReader reader,
                                                              String currentFile) throws IOException {
        TimerThread timerThread = new TimerThread(Thread.currentThread(), TIMEOUT_LINE);
        final long adjustedFileTimeout = TIMEOUT_FILE * getTotalNumberOfPatterns(causes);

        Map<FailureCause, List<FoundIndication>> resultMap = new HashMap<FailureCause, List<FoundIndication>>();
        Map<FailureCause, List<Indication>> firstOccurrences = new HashMap<FailureCause, List<Indication>>();

        timerThread.start();
        try {
            long startTime = System.currentTimeMillis();
            int currentLine = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                for (FailureCause cause : causes) {
                    for (Indication indication : cause.getIndications()) {
                        try {
                            List<Indication> wasBefore = firstOccurrences.get(cause);
                            if (wasBefore == null || !wasBefore.contains(indication)) {
                                if (processIndication(build, currentFile, resultMap, line, cause, indication)) {
                                    wasBefore = new ArrayList<Indication>();
                                    wasBefore.add(indication);
                                    firstOccurrences.put(cause, wasBefore);
                                }
                            }
                        } catch (RuntimeException e) {
                            if (e.getCause() instanceof InterruptedException) {
                                logger.warning("Timeout scanning for indication '" + indication.toString() + "'"
                                        + " for file " + currentFile + ":" + currentLine);
                            } else {
                                // This is not a timeout exception
                                throw e;
                            }
                        }
                        currentLine++;
                        timerThread.touch();
                        if (System.currentTimeMillis() - startTime > adjustedFileTimeout) {
                            logger.warning("File timeout scanning for indication '" + indication.toString() + "'"
                                    + " for file " + currentFile + ":" + currentLine);
                            return convertToFoundFailureCauses(resultMap);
                        }
                    }
                }
            }
            return convertToFoundFailureCauses(resultMap);
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
     * Calculates total number of patterns in list of causes.
     *
     * @param causes list of failure causes that we a looking for.
     * @return total number of patterns.
     */
    private static int getTotalNumberOfPatterns(List<FailureCause> causes) {
        int total = 0;
        for (FailureCause cause : causes) {
            total += cause.getIndications().size();
        }
        return total;
    }

    /**
     *
     * Updates map of found failure causes if pattern matches the line
     *
     * @param build current build
     * @param currentFile current file
     * @param causeIndicationsMap result map
     * @param line line with content
     * @param cause current cause
     * @param indication indication that should be checked
     * @return true if new indication was found
     */
    private static boolean processIndication(Run build,
                                             String currentFile,
                                             Map<FailureCause, List<FoundIndication>> causeIndicationsMap,
                                             String line,
                                             FailureCause cause,
                                             Indication indication) {
        Pattern pattern = indication.getPattern();

        if (pattern.matcher(new InterruptibleCharSequence(line)).matches()) {
            FoundIndication foundIndication = new FoundIndication(
                                                    build,
                                                    pattern.toString(),
                                                    currentFile,
                                                    ConsoleNote.removeNotes(line));


            putToMapWithList(causeIndicationsMap, cause, foundIndication);
            return true;
        }
        return false;
    }

    /**
     * Put FoundIndication to List of according FailureCause
     * @param causeIndicationsMap result map
     * @param cause Failure cause that would be used as key
     * @param foundIndication Found indication that would be pushed to map
     */
    private static void putToMapWithList(Map<FailureCause,
                                         List<FoundIndication>> causeIndicationsMap,
                                         FailureCause cause,
                                         FoundIndication foundIndication) {
        if (causeIndicationsMap.containsKey(cause)) {
            causeIndicationsMap.get(cause).add(foundIndication);
        } else {
            List<FoundIndication> foundIndications = new ArrayList<FoundIndication>();
            foundIndications.add(foundIndication);
            causeIndicationsMap.put(cause, foundIndications);
        }
    }

    /**
     * Converts from a map with a FailureCause as key
     * and a list of FoundIndications as value
     * to a list of FoundFailureCauses
     *
     * @param causes input data
     * @return List of FoundFailureCauses that was generated from input data
     */
    private static List<FoundFailureCause> convertToFoundFailureCauses(Map<FailureCause, List<FoundIndication>> causes) {
        List<FoundFailureCause> foundFailureCauses = new ArrayList<FoundFailureCause>(causes.size());

        for (Map.Entry<FailureCause, List<FoundIndication>> entry : causes.entrySet()) {
            foundFailureCauses.add(new FoundFailureCause(entry.getKey(), entry.getValue()));
        }

        return foundFailureCauses;
    }

    /**
     * Scans one file for the required multi-line pattern.
     * @param build the build we are processing.
     * @param reader the reader to read from.
     * @param currentFile the file path of the file we want to scan.
     * @return a FoundIndication if we find the pattern, null if not.
     * @throws IOException if problems occur in the reader handling.
     */
    protected FoundIndication scanMultiLineOneFile(Run build, BufferedReader reader, String currentFile)
            throws IOException {
        TimerThread timerThread = new TimerThread(Thread.currentThread(), TIMEOUT_BLOCK);
        FoundIndication foundIndication = null;
        final Pattern pattern = indication.getPattern();
        timerThread.start();
        try {
            long startTime = System.currentTimeMillis();
            char[] buf = new char[BUF_SIZE_BYTES];
            StringBuilder searchBuffer = new StringBuilder();
            int read;
            boolean firstRead = true;
            //CS IGNORE AvoidInlineConditionals FOR NEXT 1 LINES. REASON: Split up makes code less reasable.
            while ((read = reader.read(buf, 0, BUF_SIZE_BYTES - (firstRead ? 0 : OVERLAP_BYTES))) != -1) {
                try {
                    firstRead = false;
                    searchBuffer.append(buf, 0, read);
                    Matcher matcher = pattern.matcher(new InterruptibleCharSequence(searchBuffer.toString()));
                    if (matcher.find()) {
                        foundIndication = new FoundIndication(build, pattern.pattern(), currentFile,
                                removeConsoleNotes(matcher.group()));
                        break;
                    }
                    searchBuffer.delete(0, BUF_SIZE_BYTES - OVERLAP_BYTES);
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
     * @param input the input string from which to remove any console notes
     * @return the input string less console notes. Note the returned string may not contain the same line endings
     * as the input string.
     */
    private String removeConsoleNotes(final String input) {
        final List<String> cleanLines = new LinkedList<String>();
        final Scanner lineTokenizer = new Scanner(input);
        try {
            lineTokenizer.useDelimiter(Pattern.compile("[\\n\\r]"));
            while (lineTokenizer.hasNext()) {
                cleanLines.add(ConsoleNote.removeNotes(lineTokenizer.next()));
            }
        } finally {
            lineTokenizer.close();
        }
        return Joiner.on('\n').join(cleanLines);
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
