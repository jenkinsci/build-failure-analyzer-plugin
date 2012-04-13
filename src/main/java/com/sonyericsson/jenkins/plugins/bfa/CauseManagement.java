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
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.RootAction;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
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
    /**
     * The request attribute key where error messages are added.
     */
    public static final String REQUEST_CAUSE_MANAGEMENT_ERROR = "CauseManagementError";

    /**
     * Session key for the last removed {@link FailureCause} by the user. Will be removed by the index page when it
     * displays it.
     */
    public static final String SESSION_REMOVED_FAILURE_CAUSE = "removed-failureCause";

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
        Iterable<FailureCause> returnValue = null;
        try {
            returnValue = PluginImpl.getInstance().getKnowledgeBase().getShallowCauses();
        } catch (Exception e) {
            String message = "Could not fetch causes: " + e.getMessage();
            setErrorMessage(message);
        }
        return returnValue;
    }

    /**
     * Sets an error message as an attribute to the current request.
     *
     * @param message the message to set.
     * @see #getErrorMessage(org.kohsuke.stapler.StaplerRequest)
     * @see #REQUEST_CAUSE_MANAGEMENT_ERROR
     */
    private void setErrorMessage(String message) {
        Stapler.getCurrentRequest().setAttribute(REQUEST_CAUSE_MANAGEMENT_ERROR, message);
    }

    /**
     * Convenience method for jelly.
     *
     * @param request the request where the message might be.
     * @return true if there is an error message to display.
     */
    public boolean isError(StaplerRequest request) {
        return Util.fixEmpty((String)request.getAttribute(REQUEST_CAUSE_MANAGEMENT_ERROR)) != null;
    }

    /**
     * Used for getting the error message to show on the page.
     *
     * @param request the request where the message might be.
     * @return the error message to show.
     */
    public String getErrorMessage(StaplerRequest request) {
        return (String)request.getAttribute(REQUEST_CAUSE_MANAGEMENT_ERROR);
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
     * Web call to remove a {@link FailureCause}. Does a permission check for {@link PluginImpl#REMOVE_PERMISSION}.
     *
     * @param id       the id of the cause to remove.
     * @param request  the stapler request.
     * @param response the stapler response.
     * @throws IOException if so during redirect.
     */
    public void doRemoveConfirm(@QueryParameter String id, StaplerRequest request, StaplerResponse response)
            throws IOException {
        Jenkins.getInstance().checkPermission(PluginImpl.REMOVE_PERMISSION);
        id = Util.fixEmpty(id);
        if (id != null) {
            try {
                FailureCause cause = PluginImpl.getInstance().getKnowledgeBase().removeCause(id);
                if (cause != null) {
                    request.getSession(true).setAttribute(SESSION_REMOVED_FAILURE_CAUSE, cause);
                }
            } catch (Exception e) {
                //Should we use errorMessage here as well?
                throw (Failure)(new Failure(e.getMessage()).initCause(e));
            }
        }
        response.sendRedirect2("./");
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
     * The permission related to this action. For Jelly convenience.
     *
     * @return the permission.
     *
     * @see PluginImpl#UPDATE_PERMISSION
     */
    public Permission getRemovePermission() {
        return PluginImpl.REMOVE_PERMISSION;
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
