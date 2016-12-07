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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.utils.OldDataConverter;

import java.util.List;

import hudson.model.Run;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static java.lang.Math.max;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Found Indication of an unsuccessful build.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoundIndication {

    /**
     * The platform file encoding. We assume that Jenkins uses it when writing the logs.
     */
    protected static final String FILE_ENCODING = System.getProperty("file.encoding");
    private String matchingFile;
    /**
     * @deprecated, kept for backwards compatibility.
     */
    @Deprecated
    private transient Integer matchingLine;
    @Deprecated
    private transient FailureCause cause;

    private String pattern;
    private Run build;
    private String matchingString;

    /**
     * Standard constructor.
     *
     * @param build           the build of this indication.
     * @param originalPattern the original pattern we used to match.
     * @param matchingFile    the path to the file in which we found the match.
     * @param matchingString  the String that makes up the match.
     */
    public FoundIndication(Run build, String originalPattern,
                           String matchingFile, String matchingString) {
        this.pattern = originalPattern;
        this.matchingFile = matchingFile;
        this.build = build;
        this.matchingString = matchingString;
    }

    /**
     * JSON Constructor.
     *
     * @param pattern the pattern we used to match.
     * @param matchingFile the path to the file in which we found the match.
     * @param matchingString the String that makes up the match.
     */
    @JsonCreator
    public FoundIndication(@JsonProperty("pattern") String pattern,
            @JsonProperty("matchingFile") String matchingFile,
            @JsonProperty("matchingString") String matchingString) {
        this.pattern = pattern;
        this.matchingFile = matchingFile;
        this.matchingString = matchingString;
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
    public Run getBuild() {
        return build;
    }

    /**
     * Getter for the matching String.
     *
     * @return the matching String.
     */
    public String getMatchingString() {
        return matchingString;
    }

    /**
     * Getter for the first matching line (useful with multi-line build log indications.
     * @return the first line from {@code getMatchingString()}.
     */
    public String getFirstMatchingLine() {
        final Scanner scanner = new Scanner(matchingString);
        try {
            scanner.useDelimiter(Pattern.compile("[\\n\\r]"));
            return scanner.next();
        } finally {
            scanner.close();
        }
    }

    /**
     * Replaces {@link #matchingLine} with {@link #matchingString} from the text in the list at
     * {@link #matchingLine}s position. But only if {@link #matchingLine} is non null.
     *
     * @param log the build-log.
     */
    public void convertFromLineNumber(List<String> log) {
        if (matchingLine != null && log.size() >= matchingLine) {
            matchingString = log.get(max(0, matchingLine - 1)); //Log line numbering starts on 1
        }
    }

    /**
     * Called after deserialization.
     * Will schedule this indication for conversion via
     * {@link com.sonyericsson.jenkins.plugins.bfa.utils.OldDataConverter}
     * from {@link #matchingLine} to {@link #matchingString} if {@link #matchingLine} is non null.
     *
     * @return this
     */
    public Object readResolve() {
        if (matchingLine != null && (matchingString == null || matchingString.isEmpty())) {
            OldDataConverter.getInstance().convertFoundIndications(this.build);
        }
        return this;
    }

    /**
     * The old matching line number.
     *
     * @return the matching line number.
     * @deprecated since 1.3.2, 1.4.0 replaced with {@link #getMatchingString()}.
     */
    @Deprecated
    public int getMatchingLine() {
        if (matchingLine != null) {
            return matchingLine;
        } else {
            return -1;
        }
    }

    /**
     * The hash-code of the {@link #matchingString}.
     * Convenience method mostly for jelly.
     *
     * @return the hash of the line of text.
     */
    public int getMatchingHash() {
        if (matchingString != null) {
            return matchingString.hashCode();
        } else {
            return 0;
        }
    }
}
