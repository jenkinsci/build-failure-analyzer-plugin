/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication;
import hudson.model.Run;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


//CS IGNORE MagicNumber FOR NEXT 300 LINES. REASON: TestData.

/**
 * Tests for the FailureReader.
 *
 * @author Claes Elgemark &lt;claes.egemark@sonymobile.com&gt;
 */
public class FailureReaderTest {

    /**
     * Simple FailureReader used in the tests.
     */
    class TestReader extends FailureReader {

        /**
         * Standard constructor.
         * @param indication the indication for the reader
         */
        public TestReader(final Indication indication) {
            super(indication);
        }

        @Override
        public FoundIndication scan(Run build) throws IOException {
            return null;
        }

        @Override
        public FoundIndication scan(Run build, PrintStream buildLog) {
            return null;
        }
    }

    /**
     * @param indication indication that we are looking for
     * @param reader build reader
     * @param currentFile current file name
     * @return found indication
     * @throws IOException Exception
     */
    private FoundIndication scan(BuildLogIndication indication,
                                 BufferedReader reader,
                                 String currentFile) throws IOException {
        Run run = PowerMockito.mock(Run.class);

        List<FailureCause> causes = new ArrayList<FailureCause>();
        FailureCause cause = new FailureCause("test", "description");
        cause.addIndication(indication);
        causes.add(cause);

        List<FoundFailureCause> foundFailureCauses = FailureReader.scanSingleLinePatterns(
                causes,
                run,
                reader,
                currentFile);

        if (foundFailureCauses.isEmpty()) {
            return null;
        }

        assertEquals(1, foundFailureCauses.size());
        assertFalse(foundFailureCauses.get(0).getIndications().isEmpty());
        return foundFailureCauses.get(0).getIndications().get(0);
    }

    /**
     * Happy test verifying that a scan doesn't take an exceptional amount of time.
     * @throws Exception if so
     */
    @Test
    public void testScanOneFile() throws Exception {
        BufferedReader br = new BufferedReader(new StringReader("scan for me please will you!\nA second line"));

        long startTime = System.currentTimeMillis();
        FoundIndication indication = scan(new BuildLogIndication(".*scan for me please.*"), br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected long time to parse log: " + elapsedTime, elapsedTime <= 1000);
        assertNotNull("Expected to find an indication", indication);
    }

    /**
     * Test of timeout on abusive line. Should timeout on two lines
     * each timeout between 1 and 2 seconds.
     * @throws Exception if so
     */
    @Test
    public void testScanOneFileWithLineTimeout() throws Exception {
        InputStream resStream = this.getClass().getResourceAsStream("FailureReaderTest.zip");
        ZipInputStream zipStream = new ZipInputStream(resStream);
        zipStream.getNextEntry();
        BufferedReader br = new QuadrupleDupleLineReader(new BufferedReader(new InputStreamReader(zipStream)));
        long startTime = System.currentTimeMillis();
        FoundIndication indication = scan(new BuildLogIndication(".*scan for me please.*"), br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected time to parse log: " + elapsedTime, elapsedTime >= 1000 && elapsedTime <= 5000);
        assertNotNull("Expected to find an indication", indication);
    }

    /**
     * Test of timeout on abusive file. Should timeout on entire scan.
     * @throws Exception if so
     */
    @Test
    public void testScanOneFileWithFileTimeout() throws Exception {
        InputStream inStream = new ByteArrayInputStream(new byte[0]);
        for (int i = 0; i < 10; i++) {
            InputStream resStream = this.getClass().getResourceAsStream("FailureReaderTest.zip");
            ZipInputStream zipStream = new ZipInputStream(resStream);
            zipStream.getNextEntry();
            inStream = new SequenceInputStream(inStream, zipStream);
        }
        BufferedReader br = new QuadrupleDupleLineReader(new BufferedReader(new InputStreamReader(inStream)));
        long startTime = System.currentTimeMillis();
        FoundIndication indication = scan(new BuildLogIndication(".*non existing string"), br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected time to parse log: " + elapsedTime, elapsedTime >= 10000 && elapsedTime <= 12000);
        assertNull("Did not expect to find an indication", indication);
    }

    /**
     * Happy test verifying that a scan doesn't take an exceptional amount of time.
     * @throws Exception if so
     */
    @Test
    public void testScanMultiLineOneFile() throws Exception {
        FailureReader reader = new TestReader(new MultilineBuildLogIndication(".*scan for me please.*"));
        BufferedReader br = new BufferedReader(new StringReader("scan for me please will you!\nA second line"));
        long startTime = System.currentTimeMillis();
        FoundIndication indication = reader.scanMultiLineOneFile(null, br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected long time to parse log: " + elapsedTime, elapsedTime <= 1000);
        assertNotNull("Expected to find an indication", indication);
    }

    /**
     * Test of timeout on abusive line.
     * @throws Exception if so
     */
    @Test
    public void testScanMultiLineOneFileWithBlockTimeout() throws Exception {
        // Evil input + expression. Will timeout every time.
        FailureReader reader = new TestReader(new MultilineBuildLogIndication("^(([a-z])+.)+[A-Z]([a-z])+$"));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes())));
        long startTime = System.currentTimeMillis();
        FoundIndication indication = reader.scanMultiLineOneFile(null, br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected time to parse log: " + elapsedTime, elapsedTime <= 5000);
        assertNull("Did not expect to find an indication", indication);
    }

    /**
     * Test of timeout on abusive file. Should timeout on entire scan.
     * @throws Exception if so
     */
    @Test
    public void testScanMultilineOneFileWithFileTimeout() throws Exception {
        FailureReader reader = new TestReader(new MultilineBuildLogIndication(".*non existing string"));
        InputStream inStream = new ByteArrayInputStream(new byte[0]);
        for (int i = 0; i < 10; i++) {
            InputStream resStream = this.getClass().getResourceAsStream("FailureReaderTest.zip");
            ZipInputStream zipStream = new ZipInputStream(resStream);
            zipStream.getNextEntry();
            inStream = new SequenceInputStream(inStream, zipStream);
        }
        BufferedReader br = new QuadrupleDupleLineReader(new BufferedReader(new InputStreamReader(inStream)));
        long startTime = System.currentTimeMillis();
        FoundIndication indication = reader.scanMultiLineOneFile(null, br, "test");
        long elapsedTime = System.currentTimeMillis() - startTime;
        br.close();
        assertTrue("Unexpected time to parse log: " + elapsedTime, elapsedTime <= 12000);
        assertNull("Did not expect to find an indication", indication);
    }

    /**
     * Desperate attempt at making a line longer to scan.
     * When the readLine method is called it returns the line from the underlying reader
     * and constructs the same line 15 times on top of the original.
     * This so that the scanning has more to work on and can timeout
     */
    static class QuadrupleDupleLineReader extends BufferedReader {

        /**
         * Standard constructor
         *
         * @param in internal reader
         * @param sz input buffer size.
         *
         * @see BufferedReader#BufferedReader(java.io.Reader, int)
         */
        public QuadrupleDupleLineReader(BufferedReader in, int sz) {
            super(in, sz);
        }

        /**
         * Standard constructor.
         *
         * @param in internal reader.
         *
         * @see BufferedReader#BufferedReader(java.io.Reader)
         */
        public QuadrupleDupleLineReader(BufferedReader in) {
            super(in);
        }

        @Override
        public String readLine() throws IOException {
            String line = super.readLine();
            if (line == null) {
                return null;
            }

            StringBuilder str = new StringBuilder(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            str.append(line);
            return str.toString();
        }
    }
}
