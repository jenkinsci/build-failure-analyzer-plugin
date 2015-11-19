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
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Indication that parses the build log file for a pattern.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class BuildLogIndication extends Indication {

    private static final long serialVersionUID = -2889792693081908532L;
    private transient Pattern compiled = null;

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
    public Pattern getPattern() {
        if (compiled == null) {
            compiled = Pattern.compile(getUserProvidedExpression());
        }
        return compiled;
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

        /**
         * A pattern matched by all Jenkins job URL:s.
         */
        private static final Pattern URL_PATTERN = Pattern.compile(
                        "^(?:http(?:s)?://(?:[^/]+))(/.*)?/([^/]+)/([^/]+)/([^/]+)/?$");

        /**
         * The number of groups in URL_PATTERN.
         */
        private static final int NUM_OF_URL_PARTS = 4;

        /**
         * An identifier for a non-numeric build for a given project,
         * like "last build" and "last failed build".
         */
        public enum StringBuildId {
            /**
             * Last build.
             */
            LAST_BUILD("lastBuild") {
                /**
                 * @param project a project.
                 * @return the build of the given project based on this StringBuildId.
                 *
                 * @see StringBuildId#getBuild(hudson.model.AbstractProject)
                 */
                @Override
                public AbstractBuild getBuild(AbstractProject<? extends AbstractProject<?, ?>,
                                        ? extends AbstractBuild<?, ?>> project) {
                    return project.getLastBuild();
                }
            },
            /**
             * Last failed build.
             */
            LAST_FAILED_BUILD("lastFailedBuild") {
                /**
                 * @param project a project.
                 * @return the build of the given project based on this StringBuildId.
                 *
                 * @see StringBuildId#getBuild(hudson.model.AbstractProject)
                 */
                @Override
                public AbstractBuild getBuild(AbstractProject<? extends AbstractProject<?, ?>,
                        ? extends AbstractBuild<?, ?>> project) {
                    return project.getLastFailedBuild();
                }
            },
            /**
             * Last unsuccessful build.
             */
            LAST_UNSUCCESSFUL_BUILD("lastUnsuccessfulBuild") {
                /**
                 * @param project a project.
                 * @return the build of the given project based on this StringBuildId.
                 *
                 * @see StringBuildId#getBuild(hudson.model.AbstractProject)
                 */
                @Override
                public AbstractBuild getBuild(AbstractProject<? extends AbstractProject<?, ?>,
                        ? extends AbstractBuild<?, ?>> project) {
                    return project.getLastUnsuccessfulBuild();
                }
            },
            /**
             * Last successful build.
             */
            LAST_SUCCESSFUL_BUILD("lastSuccessfulBuild") {
                /**
                 * @param project a project.
                 * @return the build of the given project based on this StringBuildId.
                 *
                 * @see StringBuildId#getBuild(hudson.model.AbstractProject)
                 */
                @Override
                public AbstractBuild getBuild(AbstractProject<? extends AbstractProject<?, ?>,
                        ? extends AbstractBuild<?, ?>> project) {
                    return project.getLastSuccessfulBuild();
                }
            };

            /**
             * The name of this StringBuildId.
             */
            private final String name;

            /**
             * Private constructor.
             *
             * @param name the name of this StringBuildId.
             */
            private StringBuildId(String name) {
                this.name = name;
            }

            /**
             * Returns the name of this StringBuildId.
             *
             * @return the name of this StringBuildId.
             */
            public String getName() {
                return name;
            }

            /**
             * Returns a StringBuildId based on a given string.
             *
             * @param str a string.
             * @return the StringBuildId whose name equals str, if
             * such a StringBuildId exists; otherwise, return null.
             */
            public static StringBuildId fromString(String str) {
                if (str != null) {
                    for (StringBuildId stringBuildId : values()) {
                        if (str.equals(stringBuildId.getName())) {
                            return stringBuildId;
                        }
                    }
                }
                return null;
            }

            /**
             * Returns a build of a given project based on this StringBuildId.
             *
             * @param project a project.
             * @return the build of the given project based on this StringBuildId.
             */
            public abstract AbstractBuild getBuild(AbstractProject<? extends AbstractProject<?, ?>,
                    ? extends AbstractBuild<?, ?>> project);
        }

        @Override
        public String getDisplayName() {
            return Messages.BuildLogIndication_DisplayName();
        }

        /**
         * Tests if a text matches a pattern.
         * @param testPattern a pattern.
         * @param testText a text.
         * @param textSourceIsUrl a boolean indicating whether testText is a URL containing the text to be matched
         *                        against pattern or a text that should be matched directly against pattern.
         * @return {@link FormValidation#ok(java.lang.String) } if the pattern is valid and
         *         the string matches the pattern,
         *         {@link FormValidation#warning(java.lang.String) } if the pattern is valid and
         *         the string does not match the pattern,
         *         {@link FormValidation#error(java.lang.String) } otherwise.
         */
        public FormValidation doMatchText(
                @QueryParameter("pattern") final String testPattern,
                @QueryParameter("testText") String testText,
                @QueryParameter("textSourceIsUrl") final boolean textSourceIsUrl) {
            if (textSourceIsUrl) {
                testText = testText.replaceAll("/\\./", "/");
                Matcher urlMatcher = URL_PATTERN.matcher(testText);
                if (urlMatcher.matches()) {
                    String[] urlParts = new String[NUM_OF_URL_PARTS];
                    for (int i = 0; i < urlParts.length; i++) {
                        urlParts[i] = urlMatcher.group(i + 1);
                    }

                    AbstractBuild build = null;
                    ItemGroup getItemInstance = null;

                    if (urlParts[0] != null && urlParts[0].split("/job").length > 0) {
                        /*
                         * We matched a folders job. Let's get the jobs up to the part were the next
                         * iteration can be continued from
                         */
                        String fullFolderName = "";
                        /* The interestingJobParts string created below is meant to discard everything
                         * that comes before the first '/job' occurrent which is either nothing or the
                         * prefix from where jenkins is served, ie: http://localhost/jenkins/job/<job>/<buildNumber>
                         */
                        String interestingJobParts = urlParts[0].split("/job", 2)[-1];
                        String[] jobParts = interestingJobParts.split("/job");
                        for (String part: jobParts) {
                            fullFolderName += part;
                        }
                        getItemInstance = (ItemGroup)Jenkins.getInstance().getItemByFullName(fullFolderName);
                    } else {
                        getItemInstance = (ItemGroup)Jenkins.getInstance();
                    }

                    /*
                       Find out which of the following url types testText matches, if any,
                       and assign to build accordingly. The url types are checked in the
                       given order.
                       Type 1: .../<job>/<buildNumber>/
                       Type 2: .../<job>/<matrixInfo>/<buildNumber>/
                       Type 3: .../<job>/<buildNumber>/<matrixInfo>/
                     */

                    if (getItemInstance.getItem(urlParts[2]) instanceof AbstractProject
                            && isValidBuildId(urlParts[3])) {
                        AbstractProject project = (AbstractProject)getItemInstance.getItem(urlParts[2]);
                        build = getBuildById(project, urlParts[3]);
                    } else if (getItemInstance.getItem(urlParts[1]) instanceof MatrixProject
                            && isValidBuildId(urlParts[3])) {
                        MatrixProject project = (MatrixProject)getItemInstance.getItem(urlParts[1]);
                        MatrixConfiguration configuration = project.getItem(urlParts[2]);
                        build = getBuildById(configuration, urlParts[3]);
                    } else if (getItemInstance.getItem(urlParts[1]) instanceof MatrixProject
                            && isValidBuildId(urlParts[2])) {
                        MatrixProject matrixProject = (MatrixProject)getItemInstance.getItem(urlParts[1]);
                        MatrixConfiguration configuration = matrixProject.getItem(urlParts[3]);
                        build = getBuildById(configuration, urlParts[2]);
                    }
                    if (build != null) {
                        try {
                            final FailureReader failureReader = getFailureReader(testPattern);
                            final FoundIndication foundIndication = failureReader.scan(build);
                            if (foundIndication == null) {
                                return FormValidation.warning(Messages.StringDoesNotMatchPattern());
                            }
                            return FormValidation.okWithMarkup(foundIndication.getFirstMatchingLine());
                        } catch (IOException e) {
                            return FormValidation.error(Messages.FailedToScanFile_Error());
                        }
                    }
                }
                return FormValidation.error(Messages.InvalidURL_Error());
            } else {
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

        /**
         * Return whether a given string is a valid build id.
         *
         * @param id a string.
         * @return true if the string is a valid build id; false otherwise.
         */
        private boolean isValidBuildId(String id) {
            return id.matches("\\d+") || StringBuildId.fromString(id) != null;
        }

        /**
         * Return the build defined by a given project and id.
         *
         * @param project a project.
         * @param id a build id.
         * @return the build defined by the given project and id, or null if no build can be
         * found for the given project and id.
         */
        private AbstractBuild getBuildById(AbstractProject<? extends AbstractProject<?, ?>,
                ? extends AbstractBuild<?, ?>> project, String id) {
            if (id.matches("\\d+")) {
                return project.getBuildByNumber(Integer.parseInt(id));
            } else {
                StringBuildId stringBuildId = StringBuildId.fromString(id);
                if (stringBuildId != null) {
                    return stringBuildId.getBuild(project);
                }
                return null;
            }
        }

        /**
         * @param testPattern the test pattern for the indication passed to the failure reader
         * @return the failure reader corresponding to this descriptor
         */
        protected FailureReader getFailureReader(final String testPattern) {
            return new BuildLogFailureReader(new BuildLogIndication(testPattern));
        }
    }
}
