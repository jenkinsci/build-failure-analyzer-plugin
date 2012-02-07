/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
/**
 * FailureCause of a build..
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
public class FailureCause implements Serializable {
    private String name;
    private String description;
    private List<Indication> indications;

    /**
     * Standard data bound constructor.
     * @param name the name of this FailureCause.
     * @param description the description of this FailureCause.
     * @param indications the list of indications
     */
    @DataBoundConstructor
    public FailureCause(String name, String description, List<Indication> indications) {
        this.name = name;
        this.description = description;
        this.indications = indications;
        if (this.indications == null) {
            this.indications = new LinkedList<Indication>();
        }
    }

    /**
     * Standard constructor.
     * @param name the name of this FailureCause.
     * @param description the description of this FailureCause.
     */
    public FailureCause(String name, String description) {
        this (name, description, null);
    }

    /**
     * Default constructor.
     * <strong>Do not use this unless you are a serializer.</strong>
     */
    public FailureCause() {
    }

    /**
     * Adds an indication to the list.
     * @param indication the indication to add.
     */
    public void addIndicator(Indication indication) {
        if (indications == null) {
            indications = new LinkedList<Indication>();
        }
        indications.add(indication);
    }

    /**
     * Getter for the name.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the description.
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter for the list of indications.
     * @return the list.
     */
    public List<Indication> getIndications() {
        if (indications == null) {
            indications = new LinkedList<Indication>();
        }
        return indications;
    }
}
