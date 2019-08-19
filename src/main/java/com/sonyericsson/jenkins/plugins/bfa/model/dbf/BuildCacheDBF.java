package com.sonyericsson.jenkins.plugins.bfa.model.dbf;

import com.axis.system.jenkins.plugins.downstream.cache.BuildCache;
import hudson.Extension;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets downstream builds using the build-cache plugin. This should cover most scenarios.
 *
 * @author Stephan Pauxberger
 */
@Extension(optional = true)
public class BuildCacheDBF extends DownstreamBuildFinder {
    static {
        BuildCache.getCache();
    }
    @Override
    public List<Run<?, ?>> getDownstreamBuilds(Run build) {
        //noinspection unchecked
        return new ArrayList(BuildCache.getCache().getDownstreamBuilds(build));
    }
}
