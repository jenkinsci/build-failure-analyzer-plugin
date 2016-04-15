/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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

import hudson.model.InvisibleAction;

import javax.annotation.Nonnull;

import hudson.model.Job;
import hudson.model.Run;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Project action showing failure cause of last build, if any.
 *
 * @author ogondza
 */
@Restricted(NoExternalUse.class)
public class FailureCauseProjectAction extends InvisibleAction {

    private final Job<?, ?> job;

    /**
     * @param job A project to report.
     */
    public FailureCauseProjectAction(@Nonnull Job<?, ?> job) {
        this.job = job;
    }

    /**
     * @return Build action to report.
     */
    public FailureCauseBuildAction getAction() {
        Run<?, ?> build = job.getLastCompletedBuild();
        if (build == null) {
            return null;
        }

        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        if (action == null) {
            return null;
        }

        return action;
    }
}
