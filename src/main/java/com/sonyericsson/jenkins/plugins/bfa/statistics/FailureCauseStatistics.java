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

package com.sonyericsson.jenkins.plugins.bfa.statistics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import org.mongojack.DBRef;

import java.util.List;

/**
 * The FailureCause statistics object.
 * Contains the id of a FailureCause and its found indications.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureCauseStatistics {
    private String id;
    private List<FoundIndication> foundIndications;

    /**
     * Getter for the id.
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the FoundIndications.
     * @return the FoundIndications.
     */
    public List<FoundIndication> getIndications() {
        return foundIndications;
    }

    /**
     * Standard constructor.
     * @param id the id of the FailureCause.
     * @param indications the list of indications.
     */
    public FailureCauseStatistics(String id, List<FoundIndication> indications) {
        this.id = id;
        this.foundIndications = indications;
    }

    /**
     * JSON constructor.
     *
     * @param failureCause database reference to the FailureCause.
     * @param indications the list of indications.
     * @throws Exception if the database reference is invalid
     */
    @JsonCreator
    public FailureCauseStatistics(
            @JsonProperty("failureCause") DBRef<FailureCause, ?> failureCause,
            @JsonProperty("indications") List<FoundIndication> indications)
            throws Exception {
        if (failureCause == null) {
            throw new Exception("Unable to resolve DBRef; failureCause mapping is null");
        }
        this.id = failureCause.getId().toString();
        this.foundIndications = indications;
    }
}
