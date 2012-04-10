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

package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.RootAction;
import hudson.security.Permission;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Logger;

/**
 * Page for managing the failure causes.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension
public class CauseManagement implements RootAction {

    private static final Logger logger = Logger.getLogger(CauseManagement.class.getName());

    /**
     * Where in the Jenkins name space this action will be.
     *
     * @see #getUrlName()
     */
    public static final String URL_NAME = "failure-cause-management";
    /**
     * The reserved id for getting a new {@link FailureCause} from {@link #getDynamic(String,
     * org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)}.
     */
    public static final String NEW_CAUSE_DYNAMIC_ID = "new";
    /**
     * The pre-filled name that a new cause gets.
     */
    public static final String NEW_CAUSE_NAME = "New...";
    /**
     * The pre-filled description that a new cause gets.
     */
    public static final String NEW_CAUSE_DESCRIPTION = "Description...";
    private String errorMessage = null;

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
     * Convenience method for calling {@link PluginImpl#getImageUrl(String, String)} from jelly.
     *
     * @param size the size
     * @param name the name
     * @return the url.
     *
     * @see PluginImpl#getImageUrl(String, String)
     */
    public String getImageUrl(String size, String name) {
        return PluginImpl.getImageUrl(size, name);
    }

    /**
     * Convenience method for {@link com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase#getShallowCauses()}.
     *
     * @return the collection of causes.
     *
     * @throws Exception if communication fails.
     */
    public Iterable<FailureCause> getShallowCauses() throws Exception {
        errorMessage = null;
        Iterable<FailureCause> returnValue = null;
        try {
            returnValue = PluginImpl.getInstance().getKnowledgeBase().getShallowCauses();
        } catch (Exception e) {
            errorMessage = "Could not fetch causes: " + e.getMessage();
        }
        return returnValue;
    }

    /**
     * Convenience method for jelly.
     * @return true if there is an error message to display.
     */
    public boolean isError() {
       return Util.fixEmpty(errorMessage) != null;
    }

    /**
     * Used for getting the error message to show on the page.
     * @return the error message to show.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Dynamic Stapler URL binding. Provides the ability to navigate to a cause via for example:
     * <code>/jenkins/failure-cause-management/abf123</code>
     *
     * @param id       the id of the cause of "new" to create a new cause.
     * @param request  the request
     * @param response the response
     * @return the cause if found or null.
     *
     * @throws Exception if communication with the knowledge base failed.
     */
    public FailureCause getDynamic(String id, StaplerRequest request, StaplerResponse response) throws Exception {
        if (NEW_CAUSE_DYNAMIC_ID.equalsIgnoreCase(id)) {
            return new FailureCause(NEW_CAUSE_NAME, NEW_CAUSE_DESCRIPTION);
        } else {
            return PluginImpl.getInstance().getKnowledgeBase().getCause(id);
        }
    }

    /**
     * The "owner" of this Action. Default this would be {@link hudson.model.Hudson#getInstance()} but if the class is
     * included in some build or something we might want to be able to easier change the side panel for example.
     *
     * @return the holder of the beer.
     */
    public ModelObject getOwner() {
        return Hudson.getInstance();
    }

    /**
     * Where to redirect after the form has been saved, probably to the owner.
     *
     * @return the owner's URL or some place else to redirect the user after save.
     */
    protected String getOwnerUrl() {
        return "/";
    }

    /**
     * Provides a list of all IndicationDescriptors. For Jelly convenience.
     *
     * @return a list of descriptors.
     *
     * @see com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication.IndicationDescriptor#getAll()
     */
    public ExtensionList<Indication.IndicationDescriptor> getIndicationDescriptors() {
        return Indication.IndicationDescriptor.getAll();
    }

    /**
     * The permission related to this action. For Jelly convenience.
     *
     * @return the permission.
     *
     * @see PluginImpl#UPDATE_PERMISSION
     */
    public Permission getPermission() {
        return PluginImpl.UPDATE_PERMISSION;
    }

    /**
     * Checks if Jenkins is run from inside a HudsonTestCase. For some reason the buildQueue fails to render when run
     * under test but works fine when run with hpi:run. So the jelly file skips the inclusion of the sidepanel if we are
     * running under test to work around this problem. The check is done via looking at the class name of {@link
     * hudson.model.Hudson#getPluginManager()}.
     *
     * @return true if we are running under test.
     */
    public boolean isUnderTest() {
        return "org.jvnet.hudson.test.TestPluginManager".
                equals(Hudson.getInstance().getPluginManager().getClass().getName());
    }

    /**
     * Provides the singleton instance of this class that Jenkins has loaded. Throws an IllegalStateException if for
     * some reason the action can't be found.
     *
     * @return the instance.
     */
    public static CauseManagement getInstance() {
        for (Action action : Hudson.getInstance().getActions()) {
            if (action instanceof CauseManagement) {
                return (CauseManagement)action;
            }
        }
        throw new IllegalStateException("We seem to not have been initialized!");
    }
}
