/*
 * The MIT License
 *
 * Copyright 2014 Stellar Science Ltd Co
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
package com.sonyericsson.jenkins.plugins.bfa.tokens;

import com.google.common.base.Splitter;
import org.apache.commons.lang.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static helper methods for Token generation.
 */
public class TokenUtils {

    private TokenUtils() {}

    /**
     * Wrap some text
     * @param text some text to wrap
     * @param width the text will be wrapped to this many characters
     * @return the text lines
     */
    /* package private */ static List<String> wrap(final String text, final int width) {
        final List<String> lines = new ArrayList< String>();
        final Splitter lineSplitter = Splitter.on(Pattern.compile("\\r?\\n"));
        //Split the text into lines
        for (final String line : lineSplitter.split(text)) {
            if (width > 0) {
                final Pattern firstNonwhitespacePattern = Pattern.compile("[^\\s]");
                final Matcher firstNonwhiteSpaceMatcher = firstNonwhitespacePattern.matcher(line);
                String indent = "";
                if (firstNonwhiteSpaceMatcher.find()) {
                    indent = line.substring(0, firstNonwhiteSpaceMatcher.start());
                }
                //Wrap each line
                final String wrappedLines = WordUtils.wrap(line, width - indent.length());
                //Split the wrapped line into lines and add those lines to the result
                for (final String wrappedLine : lineSplitter.split(wrappedLines)) {
                    lines.add(indent + wrappedLine.trim());
                }
            } else {
                lines.add(line);
            }
        }
        return lines;
    }
}
