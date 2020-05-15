package com.sonyericsson.jenkins.plugins.bfa.tokens;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * Tests that the plugin is compatible with the token macro pipeline step.
 */
public class PipelineTokenTest {
    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    //CS IGNORE LineLength FOR NEXT 11 LINES. REASON: Test data.
    private static final String DECLARATIVE_PIPELINE =
                      "pipeline {\n"
                    + "  agent any\n"
                    + "  stages {\n"
                    + "    stage(\"Run declarative bfa\") {\n"
                    + "      steps {\n"
                    + "        writeFile file: 'bfa.log', text: tm('''${BUILD_FAILURE_ANALYZER, "
                                    + "noFailureText=\"No errors found - Declarative\"}''')\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n";

    //CS IGNORE LineLength FOR NEXT 6 LINES. REASON: Test data.
    private static final String SCRIPTED_PIPELINE =
                      "node {\n"
                    + "  stage(\"Run scripted bfa\") {\n"
                    + "    writeFile file: 'bfa.log', text: tm('''${BUILD_FAILURE_ANALYZER, "
                              + "noFailureText=\"No errors found - Scripted\"}''')\n"
                    + "  }\n"
                    + "}\n";

    /**
     * Tests that the plugin is run by the tm pipeline step in a declarative pipeline by writing the result to a file.
     *
     * @throws Exception If necessary
     */
    @Test
    public void declarativePipelineTokenMacro() throws Exception {
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
     * @throws Exception If necessary
     */
    @Test
    public void scriptedPipelineTokenMacro() throws Exception {
        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(SCRIPTED_PIPELINE, true));

        final WorkflowRun build = jenkinsRule.buildAndAssertSuccess(project);
        final FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(project);
        final FilePath bfaLog = workspace.child("bfa.log");

        final String bfaLogText = bfaLog.readToString();

        assertEquals("No errors found - Scripted", bfaLogText);
    }
}
