package com.sonyericsson.jenkins.plugins.bfa.statistics.LoggingWork;

import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.model.Run;

import java.util.List;

/**
 * Created by ccordenier on 08/02/2017.
 */
abstract class LoggingWork implements Runnable {

    protected List<FoundFailureCause> causes;
    protected Run build;

    /**
     * Standard Constructor.
     *
     * @param build the build to log for.
     * @param causes the causes to log.
     */
    public LoggingWork(Run build, List<FoundFailureCause> causes) {
        this.build = build;
        this.causes = causes;
    }

    /**
     * Generates the statistics.
     *
     */
    public abstract void run();
}
