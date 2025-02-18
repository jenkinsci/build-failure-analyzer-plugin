package com.sonyericsson.jenkins.plugins.bfa.tokens;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the plugin is compatible with the token macro pipeline step.
 */
@WithJenkins
class PipelineTokenTest {

    private static final String DECLARATIVE_PIPELINE =
            """
                    pipeline {
                      agent any
                      stages {
                        stage("Run declarative bfa") {
                          steps {
                            writeFile file: 'bfa.log', text: tm('''${BUILD_FAILURE_ANALYZER, \
                    noFailureText="No errors found - Declarative"}''')
                          }
                        }
                      }
                    }
                    """;

    private static final String SCRIPTED_PIPELINE =
            """
                    node {
                      stage("Run scripted bfa") {
                        writeFile file: 'bfa.log', text: tm('''${BUILD_FAILURE_ANALYZER, \
                    noFailureText="No errors found - Scripted"}''')
                      }
                    }
                    """;

    /**
     * Tests that the plugin is run by the tm pipeline step in a declarative pipeline by writing the result to a file.
     *
     * @param jenkinsRule
     *
     * @throws Exception If necessary
     */
    @Test
    void declarativePipelineTokenMacro(JenkinsRule jenkinsRule) throws Exception {
        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(DECLARATIVE_PIPELINE, true));

        final WorkflowRun build = jenkinsRule.buildAndAssertSuccess(project);
        final FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(project);
        final FilePath bfaLog = workspace.child("bfa.log");

        final String bfaLogText = bfaLog.readToString();

        assertEquals("No errors found - Declarative", bfaLogText);
    }

    /**
     * Tests that the plugin is run by the tm pipeline step in a scripted pipeline by writing the result to a file.
     *
     * @param jenkinsRule
     *
     * @throws Exception If necessary
     */
    @Test
    void scriptedPipelineTokenMacro(JenkinsRule jenkinsRule) throws Exception {
        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(SCRIPTED_PIPELINE, true));

        final WorkflowRun build = jenkinsRule.buildAndAssertSuccess(project);
        final FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(project);
        final FilePath bfaLog = workspace.child("bfa.log");

        final String bfaLogText = bfaLog.readToString();

        assertEquals("No errors found - Scripted", bfaLogText);
    }
}
