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

import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;

import java.util.LinkedList;
import java.util.List;

/**
 * The action to show the {@link FailureCause} to the user..
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCauseBuildAction implements BuildBadgeAction {
    private transient List<FailureCause> failureCauses;
    private List<FoundFailureCause> foundFailureCauses;

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
        return "../" + CauseManagement.getInstance().getUrlName();
    }

    /**
     * Getter for the FoundFailureCauses.
     *
     * @return the FoundFailureCauses.
     */
    public List<FoundFailureCause> getFoundFailureCauses() {
        return foundFailureCauses;
    }

    /**
     * Gets the image url for the summary page.
     *
     * @return the image url.
     */
    public String getImageUrl() {
        return PluginImpl.getImageUrl("48x48", PluginImpl.DEFAULT_ICON_NAME);
    }

    /**
     * Gets the image url for the badge page.
     *
     * @return the image url.
     */
    public String getBadgeImageUrl() {
        return PluginImpl.getImageUrl("16x16", PluginImpl.DEFAULT_ICON_NAME);
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
}
