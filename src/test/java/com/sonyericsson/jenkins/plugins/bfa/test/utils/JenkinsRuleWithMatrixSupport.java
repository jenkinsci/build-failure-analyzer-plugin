package com.sonyericsson.jenkins.plugins.bfa.test.utils;

import hudson.matrix.MatrixProject;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

/**
 * Jenkins Rule with extra methods for {@link MatrixProject}s.
 */
public class JenkinsRuleWithMatrixSupport extends JenkinsRule {

    /**
     * Create matrix project.
     * @return Project
     * @throws IOException Failed to save the project
     */
    public MatrixProject createMatrixProject() throws IOException {
        return jenkins.createProject(MatrixProject.class, createUniqueProjectName());
    }

    /**
     * Create matrix project.
     * @return Project
     * @param name Project name
     * @throws IOException Failed to save the project
     */
    public MatrixProject createMatrixProject(String name) throws IOException {
        return jenkins.createProject(MatrixProject.class, name);
    }
}
