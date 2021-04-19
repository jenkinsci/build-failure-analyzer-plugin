/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications Inc. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * FailureCause of a build.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureCause implements Serializable, Action, Describable<FailureCause> {
    private static final Logger logger = Logger.getLogger(FailureCause.class.getName());
    private String id;
    private String name;
    private String description;
    private String comment;
    private Date lastOccurred;
    private List<String> categories;
    private List<Indication> indications;
    private List<FailureCauseModification> modifications;

    /**
     * Standard data bound constructor.
     *
     * @param id            the id.
     * @param name          the name of this FailureCause.
     * @param description   the description of this FailureCause.
     * @param indications   the list of indications
     */
    @DataBoundConstructor
    public FailureCause(String id, String name, String description, List<Indication> indications) {
        this(id, name, description, null, null, (List<String>)null,
                indications, null);
    }

    /**
     * Standard constructor.
     *
     * @param id            the id.
     * @param name          the name of this FailureCause.
     * @param description   the description of this FailureCause.
     * @param comment       the comment of this FailureCause.
     * @param lastOccurred  the time at which this FailureCause last occurred.
     * @param categories    the categories of this FailureCause.
     * @param indications   the list of indications
     * @param modifications the modification history of this FailureCause.
     */
    public FailureCause(String id, String name, String description, String comment,
                        Date lastOccurred, String categories, List<Indication> indications,
                        List<FailureCauseModification> modifications) {
        this(id, name, description, comment, lastOccurred, Arrays.<String>asList(Util.tokenize(categories)),
                indications, modifications);
    }

    /**
     * JSON constructor.
     *
     * @param id            the id.
     * @param name          the name of this FailureCause.
     * @param description   the description of this FailureCause.
     * @param comment       the comment of this FailureCause.
     * @param lastOccurred  the last time this FailureCause occurred.
     * @param categories    the categories of this FailureCause.
     * @param indications   the list of indications
     * @param modifications the modification history of this FailureCause.
     */
    @JsonCreator
    public FailureCause(@Id @ObjectId @JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("description") String description,
                        @JsonProperty("comment") String comment,
                        @JsonProperty("occurred") Date lastOccurred,
                        @JsonProperty("categories") List<String> categories,
                        @JsonProperty("indications") List<Indication> indications,
                        @JsonProperty("modifications") List<FailureCauseModification> modifications) {
        this.id = Util.fixEmpty(id);
        this.name = name;
        this.description = description;
        this.comment = comment;
        if (lastOccurred == null) {
            this.lastOccurred = null;
        } else {
            this.lastOccurred = (Date)lastOccurred.clone();
        }
        this.categories = categories;
        this.indications = indications;
        if (this.indications == null) {
            this.indications = new LinkedList<Indication>();
        }
        this.modifications = modifications;
        if (this.modifications == null) {
            this.modifications = new LinkedList<FailureCauseModification>();
        }
    }

    /**
     * Standard constructor.
     *
     * @param name        the name of this FailureCause.
     * @param description the description of this FailureCause.
     */
    public FailureCause(String name, String description) {
        this(null, name, description, "", null, "", null, null);
    }

    /**
     * Standard constructor.
     *
     * @param name        the name of this FailureCause.
     * @param description the description of this FailureCause.
     * @param comment the comment for this FailureCause.
     */
    public FailureCause(String name, String description, String comment) {
        this(null, name, description, comment, null, "", null, null);
    }

    /**
     * Standard constructor.
     *
     * @param name        the name of this FailureCause.
     * @param description the description of this FailureCause.
     * @param comment the comment for this FailureCause.
     * @param categories    the categories of this FailureCause.
     */
    public FailureCause(String name, String description, String comment, String categories) {
        this(null, name, description, "", null, Arrays.<String>asList(Util.tokenize(categories)), null, null);
    }

    /**
     * Default constructor. <strong>Do not use this unless you are a serializer.</strong>
     */
    public FailureCause() {
    }

    /**
     * Validates this FailureCause. Checks for: {@link FailureCauseDescriptor#doCheckName(String, String)},
     * {@link FailureCauseDescriptor#doCheckDescription(String)},
     * Indications.size &gt; 0. and {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication#validate()}.
     *
     * @param newName        the name to validate
     * @param newDescription the description
     * @param newIndications the list of indications
     * @return {@link hudson.util.FormValidation#ok()} if everything is fine.
     */
    public FormValidation validate(String newName,
                                   String newDescription,
                                   List<Indication> newIndications) {
        FormValidation nameVal = getDescriptor().doCheckName(newName, id);
        if (nameVal.kind != FormValidation.Kind.OK) {
            return nameVal;
        }
        FormValidation descriptionVal = getDescriptor().doCheckDescription(newDescription);
        if (descriptionVal.kind != FormValidation.Kind.OK) {
            return descriptionVal;
        }
        if (newIndications == null || newIndications.isEmpty()) {
            return FormValidation.error("Need at least one indication for " + newName);
        }
        for (Indication indication : newIndications) {
            FormValidation validation = indication.validate();
            if (validation.kind != FormValidation.Kind.OK) {
                return validation;
            }
        }
        return FormValidation.ok();
    }

    /**
     * Form validation for {@link #description}. Checks for not empty and not "Description..."
     *
     * @param value the form value.
     * @return {@link hudson.util.FormValidation#ok()} if everything is well.
     * @deprecated Use {@link FailureCauseDescriptor#doCheckDescription(String)} instead
     */
    @Deprecated
    public FormValidation doCheckDescription(@QueryParameter final String value) {
        return getDescriptor().doCheckDescription(value);
    }

    /**
     * Form validation for {@link #name}. Checks for not empty, not "New...", {@link Jenkins#checkGoodName(String)} and
     * that it is unique based on the cache of existing causes.
     *
     * @param value the form value.
     * @return {@link hudson.util.FormValidation#ok()} if everything is well.
     * @deprecated Use {@link FailureCauseDescriptor#doCheckName(String, String)} instead
     */
    @Deprecated
    public FormValidation doCheckName(@QueryParameter final String value) {
        return getDescriptor().doCheckName(value, id);
    }

    /**
     * The form submission handler. Takes the input form and stores the data. Called by Stapler.
     *
     * @param request  the request.
     * @param response the response
     * @throws Exception if it fails to save to the knowledge base or a validation error occurs.
     */
    public synchronized void doConfigSubmit(StaplerRequest request, StaplerResponse response)
            throws Exception {
        logger.entering(getClass().getName(), "doConfigSubmit");
        Jenkins.getInstance().checkPermission(PluginImpl.UPDATE_PERMISSION);
        JSONObject form = request.getSubmittedForm();
        String newId = form.getString("id");
        newId = Util.fixEmpty(newId);
        String oldId = Util.fixEmpty(id);
        //Just some paranoid checks
        if (newId != null) {
            if (oldId != null && !newId.equals(oldId)) {
                throw new Failure("Attempt at changing the wrong cause! Expected [" + id + "] but got [" + newId + "]");
            } else if (oldId == null) {
                throw new Failure("Attempt at setting id of new cause!");
            }
        } else if (oldId != null) {
            throw new Failure("Clone attempt of cause [" + id + "]");
        }
        String newName = form.getString("name");
        String newDescription = form.getString("description");
        String newComment = form.getString("comment");
        String jsonCategories = form.optString("categories");
        if (Util.fixEmpty(jsonCategories) != null) {
            this.categories = Arrays.asList(Util.tokenize(jsonCategories));
        } else {
            this.categories = null;
        }

        Object jsonIndications = form.opt("indications");
        if (jsonIndications == null) {
            throw new Failure("You need to provide at least one indication!");
        }
        List<Indication> newIndications = request.bindJSONToList(Indication.class, jsonIndications);
        FormValidation validation = validate(newName, newDescription, newIndications);
        if (validation.kind != FormValidation.Kind.OK) {
            throw validation;
        }
        this.name = newName;
        this.description = newDescription;
        this.comment = newComment;
        this.indications = newIndications;

        String user = null;
        User current = User.current();
        if (current != null) {
            user = current.getId();
        }

        this.modifications.add(0, new FailureCauseModification(user, new Date()));

        if (newId == null) {
            PluginImpl.getInstance().getKnowledgeBase().addCause(this);
        } else {
            PluginImpl.getInstance().getKnowledgeBase().saveCause(this);
        }

        response.sendRedirect2("../");
    }

    /**
     * Adds an indication to the list.
     *
     * @param indication the indication to add.
     */
    public void addIndication(Indication indication) {
        if (indications == null) {
            indications = new LinkedList<Indication>();
        }
        indications.add(indication);
    }

    /**
     * The id.
     *
     * @return the id.
     */
    @Id
    @ObjectId
    public String getId() {
        return id;
    }

    /**
     * The id.
     *
     * @param id the id.
     */
    @Id
    @ObjectId
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter for the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter for the comment.
     *
     * @return the comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Getter for the last occurrence.
     *
     * @return the last occurrence.
     */
    public Date getLastOccurred() {
        if (lastOccurred != null) {
            return (Date)lastOccurred.clone();
        } else {
            return null;
        }
    }

    /**
     * Initiates the last occurrence if it's not already initiated
     * and then returns the date of last modification.
     * @return the last occurrence.
     */
    @JsonIgnore
    public Date getAndInitiateLastOccurred() {
        if (lastOccurred == null && id != null) {
            loadLastOccurred();
        }

        if (lastOccurred != null) {
            return (Date)lastOccurred.clone();
        } else {
            return null;
        }
    }

    /**
     * Setter for the last occurrence.
     *
     * @param lastOccurred the occurrence to set.
     */
    @DataBoundSetter
    public void setLastOccurred(Date lastOccurred) {
        if (lastOccurred == null) {
            this.lastOccurred = null;
        } else {
            this.lastOccurred = (Date)lastOccurred.clone();
        }
    }

    /**
     * Getter for the list of modifications.
     *
     * @return the modifications.
     */
    public List<FailureCauseModification> getModifications() {
        return modifications;
    }

    /**
     * Initiates the list of modifications if it's not already initiated
     * and then returns the list.
     * @return list of modifications
     */
    @JsonIgnore
    public List<FailureCauseModification> getAndInitiateModifications() {
        if ((modifications == null || modifications.isEmpty())
                && id != null) {
            initModifications();
        }
        return modifications;
    }

    /**
     * Getter for the categories.
     *
     * @return the categories.
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Setter for the comment.
     *
     * @param comment the comment
     */
    @DataBoundSetter
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Setter for the FailureCauseModifications done to this FailureCause.
     *
     * @param modifications the modifications
     */
    @DataBoundSetter
    public void setModifications(List<FailureCauseModification> modifications) {
        this.modifications = modifications;
    }

    /**
     * Returns the categories as a String, used for the view.
     *
     * @return the categories as a String.
     */
    @JsonIgnore
    public String getCategoriesAsString() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String item : categories) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(item);
        }
        return builder.toString();
    }

    /**
     * Helper method for initializing the list of FailureCauseModifications done to this FailureCause.
     */
    private void initModifications() {
        if (this.modifications == null) {
            this.modifications = new LinkedList<FailureCauseModification>();
        }

        KnowledgeBase kb = PluginImpl.getInstance().getKnowledgeBase();

        Date creationDate = kb.getCreationDateForCause(id);
        FailureCauseModification creation = new FailureCauseModification(null, creationDate);
        this.modifications.add(creation);

        FailureCause originalCause = null;
        try {
            originalCause = kb.getCause(this.id);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Got exception when loading the original FailureCause");
            // Handled in finally-clause
        } finally {
            if (originalCause == null) {
                logger.warning("Original FailureCause was null");
                return;
            }
        }

        if (originalCause.modifications == null) {
            originalCause.modifications = new LinkedList<FailureCauseModification>();
        }
        originalCause.modifications.add(creation);

        try {
            kb.saveCause(originalCause);
        } catch (Exception e) {
            logger.warning("Failed saving failure cause modification to knowledgeBase");
        }
    }

    /**
     * Gets the latest {@link FailureCauseModification} of this FailureCause.
     *
     * @return the latest modification
     */
    @JsonIgnore
    public FailureCauseModification getLatestModification() {
        List<FailureCauseModification> mods = getAndInitiateModifications();
        if (mods != null && !mods.isEmpty()) {
            FailureCauseModification latestMod = mods.get(0);
            if (latestMod.getTime().getTime() > 0) {
                return latestMod;
            }
        }
        return null;
    }

    /**
     * If we're missing information about when this FailureCause last occurred,
     * try to find an occurrence in the knowledgeBase.
     * If none is found, set the lastOccurred-attribute to the unix epoch, which symbolizes 'Never'.
     */
    private void loadLastOccurred() {
        this.lastOccurred = PluginImpl.getInstance().getKnowledgeBase().getLatestFailureForCause(this.id);
        if (lastOccurred == null) {
            lastOccurred = new Date(0);
        }
        try {
            FailureCause originalCause = PluginImpl.getInstance().getKnowledgeBase().getCause(this.id);
            originalCause.setLastOccurred(lastOccurred);
            PluginImpl.getInstance().getKnowledgeBase().saveCause(originalCause);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed updating lastOccurred", e);
        }
    }

    /**
     * Setter for the categories.
     *
     * @param categories the categories.
     */
    @DataBoundSetter
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * Getter for the list of indications.
     *
     * @return the list.
     */
    public List<Indication> getIndications() {
        if (indications == null) {
            indications = new LinkedList<Indication>();
        }
        return indications;
    }

    //CS IGNORE JavadocMethod FOR NEXT 8 LINES. REASON: The exception can be thrown.

    /**
     * Finds the {@link CauseManagement} ancestor of the {@link Stapler#getCurrentRequest() current request}.
     *
     * @return the management action or a derivative of it, or null if no management action is found.
     * @throws IllegalStateException if no ancestor is found.
     */
    @JsonIgnore
    public CauseManagement getAncestorCauseManagement() {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        if (currentRequest == null) {
            return null;
        }
        CauseManagement ancestorObject = currentRequest.findAncestorObject(CauseManagement.class);
        if (ancestorObject == null) {
            return null;
        }
        return ancestorObject;
    }

    @Override
    @JsonIgnore
    public String getIconFileName() {
        return PluginImpl.getDefaultIcon();
    }

    @Override
    @JsonIgnore
    public String getDisplayName() {
        return name;
    }

    @Override
    @JsonIgnore
    public String getUrlName() {
        return id;
    }


    @Override
    public FailureCauseDescriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(FailureCauseDescriptor.class);
    }

    /**
     * Descriptor is only used for auto completion of categories.
     */
    @Extension
    @JsonIgnoreType
    public static final class FailureCauseDescriptor extends Descriptor<FailureCause> {

        /**
         * The name of a session attribute which stores the url to the last failed build of the project from
         * whose page the Failure Cause Management page was entered.
         */
        private static final String LAST_FAILED_BUILD_URL_SESSION_ATTRIBUTE_NAME = "BFA_LAST_FAILED_BUILD_URL";

        /**
         * @return the URL to the last failed build of the project from whose page the Failure Cause Management
         * page was entered.
         */
        public String getLastFailedBuildUrl() {
            StaplerRequest staplerRequest = Stapler.getCurrentRequest();
            if (staplerRequest != null) {
                String answer = (String)staplerRequest.getSession(true).
                        getAttribute(LAST_FAILED_BUILD_URL_SESSION_ATTRIBUTE_NAME);
                if (answer != null) {
                    return answer;
                }
            }
            return "";
        }

        /**
         * Set the URL of the last failed build of the project from whose page the Failure Cause Management
         * page was entered.
         */
        public void setLastFailedBuildUrl() {
            StaplerRequest staplerRequest = Stapler.getCurrentRequest();
            if (staplerRequest != null) {
                Job project = staplerRequest.findAncestorObject(Job.class);
                if (project != null && project.getLastFailedBuild() != null) {
                    staplerRequest.getSession(true).setAttribute(LAST_FAILED_BUILD_URL_SESSION_ATTRIBUTE_NAME,
                            Hudson.getInstance().getRootUrl() + project.getLastFailedBuild().getUrl());
                } else {
                    staplerRequest.getSession(true).setAttribute(LAST_FAILED_BUILD_URL_SESSION_ATTRIBUTE_NAME, "");
                }
            }
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        /**
         * Form validation for {@link #description}. Checks for not empty and not "Description..."
         *
         * @param value the form value.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        @RequirePOST
        public FormValidation doCheckDescription(@QueryParameter final String value) {
            Jenkins.getInstance().checkPermission(PluginImpl.UPDATE_PERMISSION);
            if (Util.fixEmpty(value) == null) {
                return FormValidation.error("You should provide a description.");
            }
            if (CauseManagement.NEW_CAUSE_DESCRIPTION.equalsIgnoreCase(value.trim())) {
                return FormValidation.error("Bad description.");
            }
            return FormValidation.ok();
        }

        /**
         * Form validation for {@link #name}. Checks for not empty, not "New...",
         * {@link Jenkins#checkGoodName(String)} and
         * that it is unique based on the cache of existing causes.
         *
         * @param value the form value.
         * @param id The id (if changing an existing cause).
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        @RequirePOST
        public FormValidation doCheckName(
                @QueryParameter final String value,
                @QueryParameter final String id) {
            Jenkins.getInstance().checkPermission(PluginImpl.UPDATE_PERMISSION);
            if (Util.fixEmpty(value) == null) {
                return FormValidation.error("You must provide a name for the failure cause!");
            }
            if (CauseManagement.NEW_CAUSE_NAME.equalsIgnoreCase(value)) {
                return FormValidation.error("Reserved name!");
            }
            try {
                Jenkins.checkGoodName(value);
            } catch (Failure failure) {
                return FormValidation.error(failure, failure.getMessage());
            }
            //Use the cache it's hopefully good enough
            try {
                for (FailureCause other : PluginImpl.getInstance().getKnowledgeBase().getCauses()) {
                    if ((id == null || !id.equals(other.getId())) && value.equals(other.getName())) {
                        return FormValidation.error("There is another cause with that name.");
                    }
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to get causes list to evaluate name! ", e);
            }
            return FormValidation.ok();
        }

        /**
         * Does the auto completion for categories, matching with any category already present in the knowledge base.
         *
         * @param value the input value.
         * @return the AutoCompletionCandidates.
         */
        public AutoCompletionCandidates doAutoCompleteCategories(@QueryParameter String value) {
            return PluginImpl.getInstance().getCategoryAutoCompletionCandidates(value);
        }
    }
}
