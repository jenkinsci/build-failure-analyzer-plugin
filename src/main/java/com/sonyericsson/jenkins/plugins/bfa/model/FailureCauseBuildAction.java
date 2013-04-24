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

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.*;

import java.util.LinkedList;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The action to show the {@link FailureCause} to the user..
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@ExportedBean
public class FailureCauseBuildAction implements BuildBadgeAction {
    private transient List<FailureCause> failureCauses;
    private List<FoundFailureCause> foundFailureCauses;
    /** The url of this action. */
    public static final String URL_NAME = "bfa";
    private static final Logger logger = Logger.getLogger(FailureCauseBuildAction.class.getName());

    /**
     * Standard constructor.
     *
     * @param foundFailureCauses the FoundFailureCauses.
     */
    public FailureCauseBuildAction(List<FoundFailureCause> foundFailureCauses) {
        this.foundFailureCauses = foundFailureCauses;
    }

    @Override
    public String getIconFileName() {
        if (Hudson.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (Hudson.getInstance().hasPermission(PluginImpl.UPDATE_PERMISSION)) {
            return Messages.CauseManagement_DisplayName();
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    /**
     * Getter for the FoundFailureCauses.
     *
     * @return the FoundFailureCauses.
     */
    @Exported
    public List<FoundFailureCause> getFoundFailureCauses() {
        return foundFailureCauses;
    }

    /**
     * Gets the image url for the summary page.
     *
     * @return the image url.
     */
    public String getImageUrl() {
        return PluginImpl.getFullImageUrl("48x48", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Gets the image url for the badge page.
     *
     * @return the image url.
     */
    public String getBadgeImageUrl() {
        return PluginImpl.getFullImageUrl("16x16", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Convenience method for jelly access to PluginImpl.
     *
     * @return the PluginImpl instance.
     */
    public PluginImpl getPluginImpl() {
        return PluginImpl.getInstance();
    }

    /**
     * Called after deserialization. Converts {@link #failureCauses} if existing.
     *
     * @return this.
     */
    public Object readResolve() {
        if (failureCauses != null) {
            List<FoundFailureCause> list = new LinkedList<FoundFailureCause>();
            for (FailureCause fc : failureCauses) {
                list.add(new FoundFailureCause(fc));
            }
            foundFailureCauses = list;
            failureCauses = null;
        }
        return this;
    }
     /**
     * Used when we are directed to a FoundFailureCause beneath the build action.
     * @param token the FoundFailureCause number of this build action we are trying to navigate to.
     * @param req the stapler request.
     * @param resp the stapler response.
     * @return the correct FoundFailureCause.
     */
    public FoundFailureCause getDynamic(String token, StaplerRequest req, StaplerResponse resp) {
        try {
        int causeNumber = Integer.parseInt(token) - 1;
            if (causeNumber >= 0 && causeNumber < foundFailureCauses.size()) {
                return foundFailureCauses.get(causeNumber);
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "[BFA] Failed to parse token for getDynamic: " + token);
            return null;
        }
        logger.log(Level.WARNING, "[BFA] Unable to navigate to the FailureCause: " + token);
        return null;
    }

    /**
     * Used for the link to the failure cause management page.
     * @param req the stapler request.
     * @param resp the stapler response
     * @throws IOException if so.
     */
    public void doIndex(StaplerRequest req, StaplerResponse resp) throws IOException {
        resp.sendRedirect2("../../failure-cause-management");
    }
}
