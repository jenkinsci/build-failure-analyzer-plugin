/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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

package com.sonyericsson.jenkins.plugins.bfa;

import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.addJobBuildCausesMetric;
import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.incCounters;
import static com.sonyericsson.jenkins.plugins.bfa.MetricsManager.UNKNOWNCAUSE;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.IFailureCauseMetricData;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.StatisticsLogger;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.matrix.MatrixProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks for Indications, trying to find the Cause of a problem.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@Extension(ordinal = BuildFailureScanner.ORDINAL)
public class BuildFailureScanner extends RunListener<Run> {

    /**
     * The ordinal of this extension, one thousand below the GerritTrigger plugin.
     */
    public static final int ORDINAL = 11003;
    private static final Logger logger = Logger.getLogger(BuildFailureScanner.class.getName());
    private static final String LOG_FILE_NAME = "log";

    private static ThreadPoolExecutor threadPoolExecutor;

    /**
     * Creates the shared thread pool after extensions have been made available.
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    @SuppressWarnings("unused")
    public static void initThreadPool() {
        threadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(
                PluginImpl.getInstance().getNrOfScanThreads()
        );
    }

    @Override
    public void onStarted(Run build, TaskListener listener) {
        if (PluginImpl.shouldScan(build)
                && build.getParent().getProperty(ScannerJobProperty.class) == null) {
            try {
                build.getParent().addProperty(new ScannerJobProperty(false));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to add a ScannerJobProperty to "
                        + build.getParent().getFullDisplayName(), e);
                listener.getLogger().println("[BFA] WARNING! Failed to add the scanner property to this job.");
            }
        }
    }

    @Override
    public void onCompleted(Run run, @NonNull TaskListener listener) {
        logger.entering(getClass().getName(), "onCompleted");
        doScan(run);
    }

    private void doScan(Run build) {
        File file = new File(build.getRootDir(), ScanLogAction.FILE_NAME);
        try (
                FileOutputStream fos = new FileOutputStream(file, true);
                PrintStream scanLog = new PrintStream(fos, true, "UTF8")
        ) {
            if (PluginImpl.isSizeInLimit(build)) {
                scanIfNotScanned(build, scanLog);
            } else {
                logToScanLog(scanLog, "Log exceeds limit: " + PluginImpl.getInstance().getMaxLogSize() + "MB");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not get the causes from the knowledge base", e);
        }
    }

    /**
     * Scans the build if it should be scanned and it has not already been scanned. If configured, also reports
     * successful builds to the {@link StatisticsLogger}.
     *
     * @param build the build to scan
     * @param scanLog log to write information to
     */
    public static void scanIfNotScanned(final Run build, final PrintStream scanLog) {
        if (PluginImpl.shouldScan(build)
            && !(build.getParent() instanceof MatrixProject)) {

            if (build.getActions(FailureCauseBuildAction.class).isEmpty()
                && build.getActions(FailureCauseMatrixBuildAction.class).isEmpty()) {

                if (PluginImpl.needToAnalyze(build.getResult())) {
                    scan(build, scanLog);

                } else if (PluginImpl.getInstance().getKnowledgeBase().isSuccessfulLogging()) {
                    final List<FoundFailureCause> emptyCauseList
                        = Collections.synchronizedList(new LinkedList<FoundFailureCause>());
                    StatisticsLogger.getInstance().log(build, emptyCauseList);
                }
            }
        }
    }

    /**
     * Performs a scan of the build, adds the {@link FailureCauseBuildAction} and reports to the
     * {@link StatisticsLogger}.
     *
     * @param build    the build to scan
     * @param scanLog log to write information to.
     */
    public static void scan(Run build, PrintStream scanLog) {
        ScanLogAction scanLogAction = new ScanLogAction();
        build.addOrReplaceAction(scanLogAction);
        try {
            Collection<FailureCause> causes = PluginImpl.getInstance().getKnowledgeBase().getCauses();
            List<FoundFailureCause> foundCauseListToLog = findCauses(causes, build, scanLog);
            List<FoundFailureCause> foundCauseList;

            /* Register failed test cases as foundCauses.
             * We do not want these to be sent to the StatisticsLogger, to avoid
             * problems due to these causes not being present in the database.
             * Since StatisticsLogger spawns a background thread, we create a
             * copy of the list.
             */
            if (PluginImpl.getInstance().isTestResultParsingEnabled()) {
                foundCauseList = Collections.synchronizedList(
                        new LinkedList<FoundFailureCause>(foundCauseListToLog));
                foundCauseList.addAll(findFailedTests(build, scanLog));
            } else {
                foundCauseList = foundCauseListToLog;
            }


            List<String> fallbackCategories = PluginImpl.getInstance().getFallbackCategories();

            if (!fallbackCategories.isEmpty()) {
                // move all generic cause from the list to a second list
                List<FoundFailureCause> foundFallbackCauses = new ArrayList<>();

                for (Iterator<FoundFailureCause> iterator = foundCauseList.iterator(); iterator.hasNext();) {
                    FoundFailureCause cause = iterator.next();
                    if (!Collections.disjoint(cause.getCategories(), fallbackCategories)) {
                        iterator.remove();
                        foundFallbackCauses.add(cause);
                    }
                }

                if (!foundFallbackCauses.isEmpty()) {
                    // we have at least one generic cause
                    if (!foundCauseList.isEmpty()) {
                        logToScanLog(scanLog, "Removing generic causes");
                    } else {
                        // we have ONLY generic causes
                        foundCauseList = foundFallbackCauses;
                    }
                }
            }


            /* Strip "job/" from beginning and "/build_number/" from end */
           StringBuilder jobName = new StringBuilder(build.getUrl().replaceFirst("^job/", ""));
           String jobNameString = jobName.reverse().toString().replaceFirst("^/\\d+/", "");
           jobName = new StringBuilder(jobNameString).reverse();

           if (!foundCauseList.isEmpty()) {
               incCounters(foundCauseList, PluginImpl.getInstance().isMetricSquashingEnabled());
               addJobBuildCausesMetric(jobName.toString(), build, foundCauseList);
           } else {
               ArrayList<IFailureCauseMetricData> unknownCauses = new ArrayList<>(
                   Collections.singletonList(UNKNOWNCAUSE)
               );
               incCounters(
                   unknownCauses,
                   PluginImpl.getInstance().isMetricSquashingEnabled()
                );
               addJobBuildCausesMetric(jobName.toString(), build, unknownCauses);
           }

            FailureCauseBuildAction buildAction = new FailureCauseBuildAction(foundCauseList);
            buildAction.setBuild(build);
            build.addAction(buildAction);
            final FailureCauseDisplayData data = buildAction.getFailureCauseDisplayData();
            List<FailureCauseDisplayData> downstreamFailureCauses = data.getDownstreamFailureCauses();

            if (!downstreamFailureCauses.isEmpty()) {
                logToScanLog(scanLog, "Found downstream Failure causes ...");
                printDownstream(scanLog, downstreamFailureCauses);
            }

            /* Split slack failure cause category string from configure menu into list of strings */
            String failureCategories = PluginImpl.getInstance().getSlackFailureCategories();
            List<String> slackFailCauseCat = Arrays.<String>asList(Util.tokenize(failureCategories));

            /* Check if ALL failures are to be reported */
            boolean notifySlackAllFail = false;
            if (slackFailCauseCat.get(0).equals(PluginImpl.getInstance().DEFAULT_SLACK_FAILURE_CATEGORIES)) {
                notifySlackAllFail = true;
            }

            StatisticsLogger.getInstance().log(build, foundCauseListToLog);

            // Check slack plugin is installed
            if (Jenkins.getInstance().getPlugin("slack") != null) {
                boolean slackEnabled = PluginImpl.getInstance().isSlackNotifEnabled();
                if (slackEnabled && slackFailCauseCat != null) {
                    String buildNum = String.valueOf(build.getNumber());
                    String buildName = data.getLinks().getProjectDisplayName();
                    String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
                    createSlackMessage(foundCauseList, notifySlackAllFail, slackFailCauseCat, buildName,
                            buildNum, buildUrl, scanLog);
                }
            }
        } catch (Exception e) {
            scanLogAction.setExceptionMessage(e.toString());
            logger.log(Level.SEVERE, "Could not scan build " + build, e);
        } finally {
            scanLogAction.finished();
        }
    }

    /**
     * Function to create the message for Slack using build and BFA information.
     * @param foundCauseList - Build failure causes found in the build
     * @param notifySlackOfAllFailures - Flag to indicate if all failures should be sent to Slack
     * @param slackFailureCauseCategories - Failure causes for which to notify Slack (from BFA configuration)
     * @param buildName - Name of the build
     * @param buildNum - Build object
     * @param buildUrl - Full URL of build
     * @param scanLog - PrintStream for the build log
     * @return String Slack message with failure name, category and description if message successfully created,
     * null otherwise
     */
    public static String createSlackMessage(List<FoundFailureCause> foundCauseList,
            boolean notifySlackOfAllFailures, List<String> slackFailureCauseCategories,
            String buildName, String buildNum, String buildUrl, PrintStream scanLog) {
        boolean notifySlackOfFailure = false;
        StringBuilder bufBuildFailCause = new StringBuilder();

        /* Check if one of the failure causes for the build matches those specified in plugin's slack settings. */
        for (FoundFailureCause foundCause : foundCauseList) {
            if (notifySlackOfAllFailures) {
                //Add two new lines between found Failure Causes
                if (bufBuildFailCause.length() != 0) {
                    bufBuildFailCause.append("\n\n");
                }
                //Create list for slack message with failure Name, Category and Description from build
                bufBuildFailCause.append("*Failure Name:* ");
                bufBuildFailCause.append(foundCause.getName());
                bufBuildFailCause.append("\n");
                bufBuildFailCause.append("*Failure Categories:* ");
                bufBuildFailCause.append(foundCause.getCategories());
                bufBuildFailCause.append("\n");
                bufBuildFailCause.append("*Description:* ");
                bufBuildFailCause.append(foundCause.getDescription());
            } else {
                // Only notify the selected categories even if others occur
                List<String> categories = foundCause.getCategories();
                if (categories != null) {
                    for (String category : categories) {
                        if (failureCategoryMatches(category, slackFailureCauseCategories)) {
                            notifySlackOfFailure = true;
                            //Add two new lines between found Failure Causes
                            if (bufBuildFailCause.length() != 0) {
                                bufBuildFailCause.append("\n\n");
                            }
                            // Create list for slack message with failure Name, Category and Description from build
                            bufBuildFailCause.append("*Failure Name:* ");
                            bufBuildFailCause.append(foundCause.getName());
                            bufBuildFailCause.append("\n");
                            bufBuildFailCause.append("*Failure Categories:* ");
                            bufBuildFailCause.append(foundCause.getCategories());
                            bufBuildFailCause.append("\n");
                            bufBuildFailCause.append("*Description:* ");
                            bufBuildFailCause.append(foundCause.getDescription());

                        }
                    }
                }
            }
        }

        /* Notify slack if configured to report "ALL" failures or if one of the failure categories matched. */
        if (notifySlackOfAllFailures || notifySlackOfFailure) {
            SlackMessageProvider slack = new SlackMessageProvider();

            StringBuilder s = new StringBuilder("Job *\"" + buildName + "\"*");
            s.append(" build *#").append(buildNum).append("* FAILED due to following failure causes: \n");
            s.append(bufBuildFailCause.toString()).append("\nSee ");
            s.append(buildUrl).append(" for details.");

            slack.postToSlack(s.toString(), scanLog);
            return s.toString();
        }
        return null;
    }

    /**
     * Function to check if atleast one of the failure cause categories match the identified failure.
     * @param category - Category of the cause for the build failure
     * @param slackFailureCauseCategories - List of failure causes for which to notify slack
     * @return Boolean true if atleast one category matches, false otherwise
     */
    private static boolean failureCategoryMatches(String category, List<String> slackFailureCauseCategories) {
        for (String slackCategory : slackFailureCauseCategories) {
            if (category.trim().equalsIgnoreCase(slackCategory.trim())) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     * Adds all causes from downstream builds in recursion
     *
     * @param scanLog log to write information to.
     * @param downstreamFailureCauses the list of downstream failure causes.
     */
    private static void printDownstream(PrintStream scanLog, List<FailureCauseDisplayData> downstreamFailureCauses) {
        for (FailureCauseDisplayData displayData : downstreamFailureCauses) {
            FailureCauseDisplayData.Links links = displayData.getLinks();
            if (!displayData.getFoundFailureCauses().isEmpty()) {
                logToScanLog(scanLog, "See: " + Jenkins.getInstance().getRootUrl() + links.getBuildUrl());
                for (FoundFailureCause foundCause : displayData.getFoundFailureCauses()) {
                    String foundString = foundCause.getName();
                    if (foundCause.getCategories() != null) {
                        foundString += " from category " + foundCause.getCategories().get(0);
                    }
                    logToScanLog(scanLog, foundString);
                }
            }
            printDownstream(scanLog, displayData.getDownstreamFailureCauses());
        }
    }

    /**
     * Finds the failure causes for this build.
     *
     * @param causes   the list of possible causes.
     * @param build    the build to analyze.
     * @param scanLog the build log.
     * @return a list of found failure causes.
     */
    private static List<FoundFailureCause> findCauses(final Collection<FailureCause> causes,
                                                      final Run build, final PrintStream scanLog) {
        threadPoolExecutor.setCorePoolSize(PluginImpl.getInstance().getNrOfScanThreads());
        threadPoolExecutor.setMaximumPoolSize(PluginImpl.getInstance().getNrOfScanThreads());

        logToScanLog(scanLog, "Scanning build for known causes...");
        long start = System.currentTimeMillis();
        final List<FoundFailureCause> foundFailureCauseList = findIndications(causes, build, scanLog);

        long time = System.currentTimeMillis() - start;
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[{0}] {1}ms", new Object[]
                    {build.getFullDisplayName(),
                            String.valueOf(time), });
        }

        if (!foundFailureCauseList.isEmpty()) {
            logToScanLog(scanLog, "Found failure cause(s):");
            for (FoundFailureCause foundCause : foundFailureCauseList) {
                if (foundCause.getCategories() == null) {
                    logToScanLog(scanLog, foundCause.getName());
                } else {
                    logToScanLog(scanLog, foundCause.getName() + " from category "
                                 + foundCause.getCategories().get(0));
                }
            }

        } else {
            logToScanLog(scanLog, "No failure causes found");
        }
        logToScanLog(scanLog, "Done. " + TimeUnit.MILLISECONDS.toSeconds(time) + "s");
        return foundFailureCauseList;
    }

    private static void logToScanLog(PrintStream scanLog, String logLine) {
        scanLog.printf("%tF %<tR %s%n", new Date(), logLine);
    }

    /**
     *
     * Finds indications for all causes.
     *
     * @param causes the list of possible causes.
     * @param build current build.
     * @param scanLog build log for providing feedback.
     * @return a list of found indications, could be empty.
     */
    private static List<FoundFailureCause> findIndications(final Collection<FailureCause> causes,
                                                           final Run build,
                                                           final PrintStream scanLog) {
        final List<FailureCause> singleLineCauses = new ArrayList<FailureCause>();
        final List<FailureCause> notOnlySingleLineCauses = new ArrayList<FailureCause>();

        splitCauses(causes, singleLineCauses, notOnlySingleLineCauses);

        final List<Future<?>> scanningTasks = new ArrayList<Future<?>>(notOnlySingleLineCauses.size() + 1);
        final List<FoundFailureCause> foundFailureCauses = Collections.synchronizedList(
                new ArrayList<FoundFailureCause>());

        if (!singleLineCauses.isEmpty()) {
            scanningTasks.add(threadPoolExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    foundFailureCauses.addAll(parseSingleLineCauses(build, scanLog, singleLineCauses));
                    Thread.currentThread().setName("BFA-scanner-" + build.getFullDisplayName());
                }
            }));
        }

        for (final FailureCause cause : notOnlySingleLineCauses) {
            scanningTasks.add(threadPoolExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    final List<FoundIndication> foundIndications = new ArrayList<FoundIndication>();
                    for (final Indication indication : cause.getIndications()) {
                        Thread.currentThread().setName("BFA-scanner-"
                                + build.getFullDisplayName() + ": "
                                + cause.getName() + "-"
                                + indication.getUserProvidedExpression());

                        FoundIndication foundIndication = parseIndication(build, scanLog, indication, cause.getName());
                        if (foundIndication != null) {
                            foundIndications.add(foundIndication);
                        }
                    }

                    if (!foundIndications.isEmpty()) {
                        foundFailureCauses.add(new FoundFailureCause(cause, foundIndications));
                    }
                }
            }));
        }

        waitAllTasks(scanLog, scanningTasks);

        return foundFailureCauses;
    }

    /**
     *
     * Wait all scanning tasks to be completed. Cancel all of them if InterruptedException happened.
     *
     * @param scanLog build log for providing feedback.
     * @param scanningTasks List of scheduled scanning tasks.
     */
    private static void waitAllTasks(PrintStream scanLog, List<Future<?>> scanningTasks) {
        try {
            for (Future<?> scanningTask : scanningTasks) {
                try {
                    scanningTask.get();
                } catch (ExecutionException e) {
                    logToScanLog(scanLog, "task failed due exception: " + e);
                }
            }
        } catch (InterruptedException e) {
            logToScanLog(scanLog, "was interrupted: " + e);
            for (Future<?> scanningTask : scanningTasks) {
                scanningTask.cancel(true);
            }
            logToScanLog(scanLog, "all bfa tasks were cancelled");
        }
    }

    /**
     *
     * Split list of causes in two part: causes with single line indication only and others.
     *
     * @param causes All causes.
     * @param singleLineCauses Collection to put list of causes with single line indication.
     * @param notOnlySingleLineCauses Collection to put other causes.
     */
    private static void splitCauses(Collection<FailureCause> causes,
                                    List<FailureCause> singleLineCauses,
                                    List<FailureCause> notOnlySingleLineCauses) {
        for (FailureCause cause : causes) {
            boolean atLeastOneNonSignalLine = false;
            for (Indication indication : cause.getIndications()) {
                if (indication instanceof MultilineBuildLogIndication) {
                    atLeastOneNonSignalLine = true;
                    break;
                }
            }

            if (atLeastOneNonSignalLine) {
                notOnlySingleLineCauses.add(cause);
            } else {
                singleLineCauses.add(cause);
            }
        }
    }

    /**
     *
     * Parse any indication.
     *
     * @param build current build.
     * @param scanLog build log for providing feedback.
     * @param indication indication to be found.
     * @param causeName name of cause this indication belongs to.
     * @return a found indication, null otherwise.
     */
    private static FoundIndication parseIndication(Run build,
                                                   PrintStream scanLog,
                                                   Indication indication,
                                                   String causeName) {
        long start = System.currentTimeMillis();

        final FoundIndication foundIndication = findIndication(indication, build, scanLog);
        if (foundIndication != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "[{0}] [{1}] {2}ms", new Object[]{build.getFullDisplayName(),
                        causeName,
                        String.valueOf(System.currentTimeMillis() - start), });
            }
        }

        return foundIndication;
    }

    /**
     *
     * Parses all causes with only single line indications.
     *
     * @param build current build.
     * @param scanLog build log for providing feedback.
     * @param singleLineCauses list of causes to be found.
     * @return a list of causes with found indication, could be empty.
     */
    private static List<FoundFailureCause> parseSingleLineCauses(Run build,
                                              PrintStream scanLog,
                                              List<FailureCause> singleLineCauses) {
        final List<FoundFailureCause> foundFailureCauses = new ArrayList<FoundFailureCause>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(build.getLogReader());
            foundFailureCauses.addAll(
                    FailureReader.scanSingleLinePatterns(
                            singleLineCauses,
                            build,
                            reader,
                            LOG_FILE_NAME));
        } catch (Exception e) {
            logToScanLog(scanLog, "Exception during parsing file: " + e);
            ScanLogAction logAction = build.getAction(ScanLogAction.class);
            if (logAction != null && logAction.getExceptionMessage() == null) {
                logAction.setExceptionMessage(e.toString());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close the reader. ", e);
                }
            }
        }
        return foundFailureCauses;
    }

    /**
     * Finds out if this indication matches the build.
     *
     * @param indication the indication to look for.
     * @param build      the build to analyze.
     * @param scanLog   the build log.
     * @return an indication if one is found, null otherwise.
     */
    private static FoundIndication findIndication(Indication indication, Run build, PrintStream scanLog) {
        FailureReader failureReader = indication.getReader();
        return failureReader.scan(build, scanLog);
    }

    /**
     * Finds the failed tests reported by this build
     *
     * @param build    the build to analyze.
     * @param scanLog the build log.
     * @return a list of found failure causes based on the test results.
     */
    private static List<FoundFailureCause> findFailedTests(final Run build, final PrintStream scanLog) {
        final List<FoundFailureCause> failedTestList =
            Collections.synchronizedList(new LinkedList<FoundFailureCause>());
        final List<AbstractTestResultAction> testActions =
            build.getActions(AbstractTestResultAction.class);

        for (AbstractTestResultAction testAction : testActions) {
            List<? extends TestResult> failedTests = testAction.getFailedTests();
            for (TestResult test : failedTests) {
                logToScanLog(scanLog, "Found failed test case: " + test.getName());
                FailureCause failureCause = new FailureCause(null,
                        test.getName(), test.getErrorStackTrace(), "", null,
                        PluginImpl.getInstance().getTestResultCategories(), null, null);
                FoundFailureCause foundFailureCause = new FoundFailureCause(failureCause);
                failedTestList.add(foundFailureCause);
            }
        }

        return failedTestList;
    }
}
