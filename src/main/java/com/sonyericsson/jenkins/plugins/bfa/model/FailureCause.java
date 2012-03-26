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

import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import hudson.util.FormValidation;
import net.vz.mongodb.jackson.Id;
import net.vz.mongodb.jackson.ObjectId;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;



/**
 * FailureCause of a build.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCause implements Serializable {
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
        this.id = id;
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
     * Validates this FailureCause.
     * Checks for:
     * <ul>
     *     <li>Name empty.</li>
     *     <li>Name unique.</li>
     *     <li>Indications.size > 0.</li>
     *     <li>{@link com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication#validate()}.</li>
     * </ul>
     *
     * @param allCauses the list of all configured causes to see if there are any duplicated names.
     * @return {@link hudson.util.FormValidation#ok()} if everything is fine.
     */
    public FormValidation validate(List<FailureCause> allCauses) {
        if (name == null || name.isEmpty()) {
            return FormValidation.error("You must provide a name for the failure cause!");
        }
        for (FailureCause failureCause : allCauses) {
            if (failureCause != this && failureCause.getName().equals(name)) {
                return FormValidation.error("Duplicated name for " + name);
            }
        }
        if (indications == null || indications.isEmpty()) {
            return FormValidation.error("Need at least one indication for " + name);
        }
        for (Indication indication : indications) {
            FormValidation validation = indication.validate();
            if (validation.kind != FormValidation.Kind.OK) {
                return validation;
            }
        }
        return FormValidation.ok();
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
}
