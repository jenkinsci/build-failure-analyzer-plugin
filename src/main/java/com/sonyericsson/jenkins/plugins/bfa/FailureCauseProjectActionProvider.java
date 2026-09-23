package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseProjectAction;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides the project-level failure cause action after other job-page action fragments.
 */
@Extension(ordinal = FailureCauseProjectActionProvider.LOW_PRIORITY)
public class FailureCauseProjectActionProvider extends TransientActionFactory<Job> {

    /**
     * Render the failure cause summary after the default-priority job page fragments.
     */
    public static final int LOW_PRIORITY = -1000;

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(Job target) {
        if (PluginImpl.shouldScan(target)) {
            return Collections.singleton(new FailureCauseProjectAction(target));
        } else {
            return Collections.emptyList();
        }
    }
}
