package com.sonyericsson.jenkins.plugins.bfa.test.utils;

import hudson.matrix.MatrixProject;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

/**
 * Jenkins Rule with extra methods for {@link MatrixProject}s.
 */
public final class MatrixSupport {

    private MatrixSupport() {
        // hidden
    }

    /**
     * Create matrix project.
     *
     * @param jenkins
     *
     * @return Project
     *
     * @throws IOException Failed to save the project
     */
    public static MatrixProject createMatrixProject(JenkinsRule jenkins) throws IOException {
        return jenkins.createProject(MatrixProject.class, "test" + jenkins.jenkins.getItems().size());
    }

    /**
     * Create matrix project.
     *
     * @param jenkins
     * @param name Project name
     *
     * @return Project
     *
     * @throws IOException Failed to save the project
     */
    public static MatrixProject createMatrixProject(JenkinsRule jenkins, String name) throws IOException {
        return jenkins.createProject(MatrixProject.class, name);
    }
}
