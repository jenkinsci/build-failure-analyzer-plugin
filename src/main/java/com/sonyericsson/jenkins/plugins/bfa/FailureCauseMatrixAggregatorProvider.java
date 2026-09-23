package com.sonyericsson.jenkins.plugins.bfa;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;

/**
 * Registers the {@link FailureCauseMatrixAggregator} to be used for matrix
 * builds where the plugin should scan.
 */
@Extension
public class FailureCauseMatrixAggregatorProvider implements MatrixAggregatable {

    @Override
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        if (PluginImpl.shouldScan(build)) {
            return new FailureCauseMatrixAggregator(build, launcher, listener);
        }
        return null;
    }
}
