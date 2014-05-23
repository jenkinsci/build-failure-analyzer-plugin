/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonyericsson.jenkins.plugins.bfa.utils;

import java.io.IOException;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional=true)
public class FailureCauseTokenMacro extends DataBoundTokenMacro {

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("BFA_CAUSE");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        FailureCauseBuildAction action = context.getAction(FailureCauseBuildAction.class);
        FailureCauseMatrixBuildAction matrixAction = context.getAction(FailureCauseMatrixBuildAction.class);

        if (action != null) {
            FailureCauseDisplayData data = action.getFailureCauseDisplayData();
            if (data.getDownstreamFailureCauses().isEmpty() && data.getFoundFailureCauses().isEmpty()) {
                return "";
            }
            return textForFailureCauseDisplayData(data, 0);
        }

        if (matrixAction != null) {
            return textForMatrix(matrixAction);
        }

        // If there are no found failures, return an empty string.
        return "";
    }

    private String textForMatrix(FailureCauseMatrixBuildAction matrixAction) {
        StringBuilder builder = new StringBuilder();

        for (MatrixRun run: matrixAction.getRunsWithAction()) {
            FailureCauseDisplayData data = FailureCauseMatrixBuildAction.getFailureCauseDisplayData(run);

            if (data.getDownstreamFailureCauses().isEmpty() && data.getFoundFailureCauses().isEmpty()) {
                return "";
            }
            builder.append("Matrix Build: " + run.getFullDisplayName());
            builder.append(System.getProperty("line.separator"));
            builder.append(textForFailureCauseDisplayData(data, 1));
        }

        return builder.toString();
    }

    private String textForFailureCauseDisplayData(FailureCauseDisplayData data, int depth) {
        StringBuilder builder = new StringBuilder();

        builder.append("----");
        builder.append(System.getProperty("line.separator"));

        if (data.getFoundFailureCauses().isEmpty() && data.getDownstreamFailureCauses().isEmpty()) {
            builder.append(indentForDepth(depth) + "No identified problem");
            builder.append(System.getProperty("line.separator"));
        } else {
            for (FoundFailureCause cause : data.getFoundFailureCauses()) {
                builder.append(textForFailureCause(cause, depth + 1));
            }
            builder.append("----");
            builder.append(System.getProperty("line.separator"));

            if (!data.getDownstreamFailureCauses().isEmpty()) {
                builder.append(indentForDepth(depth) + "- Downstream Builds:");
                builder.append(System.getProperty("line.separator"));
                builder.append("----");
                builder.append(System.getProperty("line.separator"));

                for (FailureCauseDisplayData downstream: data.getDownstreamFailureCauses()) {
                    builder.append(indentForDepth(depth + 1)
                            + "Build: " + downstream.getLinks().getProjectDisplayName()
                            + "(" + downstream.getLinks().getBuildDisplayName() + ")");
                    builder.append(System.getProperty("line.separator"));
                    builder.append(textForFailureCauseDisplayData(downstream, depth + 1));
                    builder.append("----");
                    builder.append(System.getProperty("line.separator"));
                }
            }
        }

        return builder.toString();
    }

    private String textForFailureCause(FoundFailureCause cause, int depth) {
        StringBuilder builder = new StringBuilder();

        builder.append(indentForDepth(depth) + "- Cause: " + cause.getName());
        builder.append(System.getProperty("line.separator"));
        builder.append(indentForDepth(depth) + "- Description: " + cause.getDescription());
        builder.append(System.getProperty("line.separator"));

        return builder.toString();
    }

    private String indentForDepth(int depth) {
        return StringUtils.repeat(" ", depth);
    }
}
