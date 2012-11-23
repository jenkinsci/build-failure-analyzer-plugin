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
package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.BuildLogFailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import hudson.Extension;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import java.util.regex.PatternSyntaxException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Indication that parses the build log file for a pattern.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class BuildLogIndication extends Indication {
    /**
     * Standard constructor.
     *
     * @param pattern the string value to search for.
     */
    @DataBoundConstructor
    public BuildLogIndication(String pattern) {
        super(pattern);
    }

    /**
     * Default constructor.
     * <strong>Do not use this unless you are a serializer.</strong>
     */
    public BuildLogIndication() {
    }

    @Override
    public FailureReader getReader() {
        return new BuildLogFailureReader(this);
    }

    @Override
    public IndicationDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BuildLogIndicationDescriptor.class);
    }

    /**
     * The descriptor.
     */
    @Extension
    public static class BuildLogIndicationDescriptor extends IndicationDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.BuildLogIndication_DisplayName();
        }

        /**
         * Tests if a string matches a pattern.
         * @param testPattern the pattern.
         * @param testText the string.
         * @return {@link FormValidation#ok(java.lang.String) } if the pattern is valid and
         *         the string matches the pattern,
         *         {@link FormValidation#warning(java.lang.String) } if the pattern is valid and
         *         the string does not match the pattern,
         *         {@link FormValidation#error(java.lang.String) } otherwise.
         */
        public FormValidation doMatchText(
                @QueryParameter("pattern") final String testPattern,
                @QueryParameter("testText") final String testText) {
            try {
                if (testText.matches(testPattern)) {
                    return FormValidation.ok(Messages.StringMatchesPattern());
                }
                return FormValidation.warning(Messages.StringDoesNotMatchPattern());
            } catch (PatternSyntaxException e) {
                return FormValidation.error(Messages.InvalidPattern_Error());
            }
        }
    }
}
