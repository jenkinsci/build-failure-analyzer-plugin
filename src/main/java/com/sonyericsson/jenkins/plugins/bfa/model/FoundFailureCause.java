/*
 * The MIT License
 *
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

import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;

import java.util.LinkedList;
import java.util.List;

/**
 * Found Failure Cause of a build.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class FoundFailureCause {
    private String name;
    private String description;
    private List<FoundIndication> indications;

    /**
     * Standard constructor.
     *
     * @param originalCause the original FailureCause.
     */
    public FoundFailureCause(FailureCause originalCause) {
        this.name = originalCause.getName();
        this.description = originalCause.getDescription();
        this.indications = new LinkedList<FoundIndication>();
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
     * Getter for the list of found indications.
     *
     * @return the list.
     */
    public List<FoundIndication> getIndications() {
        if (indications == null) {
            indications = new LinkedList<FoundIndication>();
        }
        return indications;
    }
    /**
     * Adds a found indication to the list.
     *
     * @param indication the indication to add.
     */
    public void addIndication(FoundIndication indication) {
        if (indications == null) {
            indications = new LinkedList<FoundIndication>();
        }
        indications.add(indication);
    }

    /**
     * Adds a list of FoundIndications to this cause.
     * @param foundIndications the list of FoundIndications.
     */
    public void addIndications(List<FoundIndication> foundIndications) {
        if (indications == null) {
            indications = new LinkedList<FoundIndication>();
        }
        indications.addAll(foundIndications);
    }
}
