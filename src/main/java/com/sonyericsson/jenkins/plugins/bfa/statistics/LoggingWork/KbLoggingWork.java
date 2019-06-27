package com.sonyericsson.jenkins.plugins.bfa.statistics.LoggingWork;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ccordenier on 08/02/2017.
 */
public class KbLoggingWork extends LoggingWork {
    private static final Logger logger = Logger.getLogger(KbLoggingWork.class.getName());

    /**
     * Standard constructor.
     *
     * @param build the build to analyse
     * @param causes the list of found causes
     */
    public KbLoggingWork(Run build, List<FoundFailureCause> causes) {
        super(build, causes);
    }

    @Override
    public void run() {
        String projectName = build.getParent().getFullName();
        int buildNumber = build.getNumber();
        String displayName = build.getDisplayName();
        Date startingTime = build.getTime();
        long duration = build.getDuration();
        List<String> triggerCauses = new LinkedList<String>();
        for (Object o : build.getCauses()) {
            triggerCauses.add(o.getClass().getSimpleName());
        }
        String nodeName = "NoNodeInformation";
        if (build instanceof AbstractBuild) {
            AbstractBuild abstractBuild = (AbstractBuild)build;
            Node node = abstractBuild.getBuiltOn();
            if (node != null) {
                nodeName = node.getNodeName();
            }
        }
        int timeZoneOffset = TimeZone.getDefault().getRawOffset();
        String master;

        String result = "Unknown";
        final Result buildResult = build.getResult();
        if (buildResult != null) {
            result = buildResult.toString();
        }

        List<FailureCauseStatistics> failureCauseStatistics = new LinkedList<FailureCauseStatistics>();
        List<String> causeIds = new LinkedList<String>();
        for (FoundFailureCause cause : causes) {
            FailureCauseStatistics stats = new FailureCauseStatistics(cause.getId(), cause.getIndications());
            failureCauseStatistics.add(stats);
            causeIds.add(cause.getId());
        }

        master = BfaUtils.getMasterName();
        Cause.UpstreamCause uc = (Cause.UpstreamCause)build.getCause(Cause.UpstreamCause.class);
        Statistics.UpstreamCause suc = new Statistics.UpstreamCause(uc);
        Statistics obj = new Statistics(projectName, buildNumber, displayName, startingTime, duration,
                triggerCauses, nodeName, master, timeZoneOffset, result, suc,
                failureCauseStatistics);

        PluginImpl p = PluginImpl.getInstance();
        KnowledgeBase kb = p.getKnowledgeBase();
        try {
            kb.saveStatistics(obj);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't save statistics: ", e);
        }

        kb.updateLastSeen(causeIds, startingTime);
    }
}
