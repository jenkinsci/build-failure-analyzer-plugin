package com.sonyericsson.jenkins.plugins.bfa.statistics.LoggingWork;

import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;

import com.timgroup.statsd.StatsDClient;
import hudson.model.Run;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ccordenier on 08/02/2017.
 */
public class StatsdLoggingWork extends LoggingWork {
    private static final Logger logger = Logger.getLogger(StatsdLoggingWork.class.getName());

    private StatsDClient statsClient;

    /**
     * Standard constructor.
     *
     * @param build the build to analyse
     * @param causes the list of found causes
     * @param statsClient the client to use to send statistics
     */
    public StatsdLoggingWork(Run build, List<FoundFailureCause> causes, StatsDClient statsClient) {
        super(build, causes);
        this.statsClient = statsClient;
    }

    @Override
    public void run() {
        for (FoundFailureCause cause : causes) {
            String key = String.format("%s.causes.%s", build.getParent().getFullName(), cause.getSlugName());
            logger.log(Level.INFO, String.format("Sending key %s", key));
            statsClient.incrementCounter(key);
        }
    }
}
