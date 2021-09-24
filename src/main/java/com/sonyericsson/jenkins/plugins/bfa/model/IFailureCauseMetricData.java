package com.sonyericsson.jenkins.plugins.bfa.model;

import java.util.List;

public interface IFailureCauseMetricData {
    /**
     * Getter for the name.
     * @return the name
     */
    String getName();

    /**
     * Getter for the categories.
     * @return the categories
     */
    List<String> getCategories();
}
