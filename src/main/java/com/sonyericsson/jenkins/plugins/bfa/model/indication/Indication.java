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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import jregex.Pattern;
import jregex.PatternSyntaxException;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreType;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import java.io.Serializable;

/**
 * Indication that can match a search string for a specific reader.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Indication implements Describable<Indication>, Serializable {

    private String pattern;
    private transient Pattern compiled = null;

    /**
     * @param pattern the String value.
     */
    @DataBoundConstructor
    @JsonCreator
    public Indication(@JsonProperty("pattern") String pattern) {
        this.pattern = pattern;
    }

    /**
     * Default constructor. <strong>Do not use this unless you are a serializer.</strong>
     */
    protected Indication() {
    }

    /**
     * Gets a FailureReader used for finding this indication.
     * @return a FailureReader.
     */
    public abstract FailureReader getReader();

    /**
     * Checks if the indication is correctly configured.
     * Default implementation checks for pattern compilation errors.
     * Override this method to provide more validation.
     *
     * @return {@link hudson.util.FormValidation#ok()} if everything is well.
     * @see IndicationDescriptor#doHelp(org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)
     */
    public FormValidation validate() {
        return IndicationDescriptor.checkPattern(pattern);
    }

    /**
     * Getter for the pattern to match.
     *
     * @return the pattern to match.
     */
    public Pattern getPattern() {
        if (compiled == null) {
            compiled = new Pattern(this.pattern);
        }
        return compiled;
    }

    @Override
    public String toString() {
        return pattern.toString();
    }

    /**
     * The descriptor for this indicator.
     */
    @JsonIgnoreType
    public abstract static class IndicationDescriptor extends Descriptor<Indication> {

        /**
         * Provides a list of all registered descriptors of this type.
         *
         * @return the list of descriptors.
         */
        public static ExtensionList<IndicationDescriptor> getAll() {
            return Hudson.getInstance().getExtensionList(IndicationDescriptor.class);
        }

        /**
         * Checks that the pattern is a valid regexp.
         *
         * @param value the pattern to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public static FormValidation checkPattern(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Please provide a pattern!");
            }
            try {
                new Pattern(value);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error("Bad syntax! " + e.getMessage());
            } catch (Exception e) {
                return FormValidation.warning("Unpredicted error. " + e.getMessage());
            }
        }

        /**
         * Checks that the pattern is a valid regexp.
         *
         * @param value the pattern to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         * @see #checkPattern(String)
         */
        public FormValidation doCheckPattern(@QueryParameter String value) {
            return checkPattern(value);
        }
    }

}
