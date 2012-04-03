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
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Failure;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import net.vz.mongodb.jackson.Id;
import net.vz.mongodb.jackson.ObjectId;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * FailureCause of a build.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCause implements Serializable, Action {
    private static final Logger logger = Logger.getLogger(FailureCause.class.getName());
    private String id;
    private String name;
    private String description;
    private List<Indication> indications;

    /**
     * Standard data bound constructor.
     *
     * @param id          the id.
     * @param name        the name of this FailureCause.
     * @param description the description of this FailureCause.
     * @param indications the list of indications
     */
    @DataBoundConstructor
    @JsonCreator
    public FailureCause(@Id @ObjectId String id, @JsonProperty("name") String name, @JsonProperty("description")
    String description, @JsonProperty("indications") List<Indication> indications) {
        this.id = Util.fixEmpty(id);
        this.name = name;
        this.description = description;
        this.indications = indications;
        if (this.indications == null) {
            this.indications = new LinkedList<Indication>();
        }
    }

    /**
     * Standard constructor.
     *
     * @param name        the name of this FailureCause.
     * @param description the description of this FailureCause.
     */
    public FailureCause(String name, String description) {
        this(null, name, description, null);
    }

    /**
     * Default constructor. <strong>Do not use this unless you are a serializer.</strong>
     */
    public FailureCause() {
    }

    /**
     * Validates this FailureCause. Checks for: {@link #doCheckName(String)}, {@link #doCheckDescription(String)},
     * Indications.size > 0. and {@link com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication#validate()}.
     *
     * @param newName        the name to validate
     * @param newDescription the description
     * @param newIndications the list of indications
     * @return {@link hudson.util.FormValidation#ok()} if everything is fine.
     */
    public FormValidation validate(String newName,
                                   String newDescription,
                                   List<Indication> newIndications) {
        FormValidation nameVal = doCheckName(newName);
        if (nameVal.kind != FormValidation.Kind.OK) {
            return nameVal;
        }
        FormValidation descriptionVal = doCheckDescription(newDescription);
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
     */
    public FormValidation doCheckDescription(@QueryParameter final String value) {
        if (Util.fixEmpty(value) == null) {
            return FormValidation.error("You should provide a description.");
        }
        if (CauseManagement.NEW_CAUSE_DESCRIPTION.equalsIgnoreCase(value.trim())) {
            return FormValidation.error("Bad description.");
        }
        return FormValidation.ok();
    }

    /**
     * Form validation for {@link #name}. Checks for not empty, not "New...", {@link Jenkins#checkGoodName(String)} and
     * that it is unique based on the cache of existing causes.
     *
     * @param value the form value.
     * @return {@link hudson.util.FormValidation#ok()} if everything is well.
     */
    public FormValidation doCheckName(@QueryParameter final String value) {
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
                if (id == null || !id.equals(other.getId())) {
                    if (value.equals(other.getName())) {
                        return FormValidation.error("There is another cause with that name.");
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get causes list to evaluate name! ", e);
        }
        return FormValidation.ok();
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
        Object jsonIndications = form.opt("indications");
        if (indications == null) {
            throw new Failure("You need to provide at least one indication!");
        }
        List<Indication> newIndications = request.bindJSONToList(Indication.class, jsonIndications);
        FormValidation validation = validate(newName, newDescription, newIndications);
        if (validation.kind != FormValidation.Kind.OK) {
            throw validation;
        }
        this.name = newName;
        this.description = newDescription;
        this.indications = newIndications;
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
     * @return the management action or a derivative of it.
     *
     * @throws IllegalStateException if no ancestor is found.
     */
    @JsonIgnore
    public CauseManagement getAncestorCauseManagement() {
        CauseManagement ancestorObject = Stapler.getCurrentRequest().findAncestorObject(CauseManagement.class);
        if (ancestorObject != null) {
            return ancestorObject;
        } else {
            throw new IllegalStateException("getAncestorCauseManagement must be called within the scope of a "
                    + "StaplerRequest with a CauseManagement ancestor!");
        }
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
}
