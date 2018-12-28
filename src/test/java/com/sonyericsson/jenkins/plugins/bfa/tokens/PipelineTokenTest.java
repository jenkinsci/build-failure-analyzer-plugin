package com.sonyericsson.jenkins.plugins.bfa.tokens;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * Tests that the plugin is compatible with the token macro pipeline step.
 */
public class PipelineTokenTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    private static final String BUILD_SCRIPT =
  "pipeline {\n"
+ "    agent any\n"
+ "    stages {\n"
+ "        stage(\"Run bfa\") {\n"
+ "            steps {\n"
+ "                writeFile file: 'bfa.log', text: tm('''${BUILD_FAILURE_ANALYZER, noFailureText=\"None Found\"}''')\n"
+ "            }\n"
+ "        }\n"
+ "    }\n"
+ "}\n";

    /**
     * Tests that the plugin is run by the tm pipeline step and writes the result to a file.
     *
     * @throws Exception If necessary
     */
    @Test
    public void pipelineEcho() throws Exception {
        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(BUILD_SCRIPT, true));

        final WorkflowRun build = jenkinsRule.buildAndAssertSuccess(project);
        final FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(project);
        final FilePath bfaLog = workspace.child("bfa.log");

        final String bfaLogText = bfaLog.readToString();

        assertEquals("None Found", bfaLogText);
    }
}
