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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sonyericsson.jenkins.plugins.bfa.graphs.ComputerGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.graphs.ProjectGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureReader;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.StatisticsLogger;
import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;

/**
 * Looks for Indications, trying to find the Cause of a problem.
 *
 * @author Tomas Westling &lt;thomas.westling@sonyericsson.com&gt;
 */
@Extension(ordinal = BuildFailureScanner.ORDINAL)
public class BuildFailureScanner extends RunListener<AbstractBuild> {

    /**
     * The ordinal of this extension, one thousand below the GerritTrigger plugin.
     */
    public static final int ORDINAL = 11003;
    private static final Logger logger = Logger.getLogger(BuildFailureScanner.class.getName());

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        if (PluginImpl.shouldScan(build)
                && build.getProject().getProperty(ScannerJobProperty.class) == null) {
            try {
                build.getProject().addProperty(new ScannerJobProperty(false));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to add a ScannerJobProperty to "
                        + build.getProject().getFullDisplayName(), e);
                listener.getLogger().println("[BFA] WARNING! Failed to add the scanner property to this job.");
            }
        }
    }

    @Override
    public void onCompleted(AbstractBuild build, TaskListener listener) {
        logger.entering(getClass().getName(), "onCompleted");
        scanIfNotScanned(build, listener.getLogger());
    }

    /**
     * Scans the build if it should be scanned and it has not already been scanned. If configured, also reports
     * successful builds to the {@link StatisticsLogger}.
     *
     * @param build the build to scan
     * @param buildLog log to write information to
     */
    public static void scanIfNotScanned(final AbstractBuild build, final PrintStream buildLog) {
        if (PluginImpl.shouldScan(build)
            && !(build.getProject() instanceof MatrixProject)) {

            if (build.getActions(FailureCauseBuildAction.class).isEmpty()
                && build.getActions(FailureCauseMatrixBuildAction.class).isEmpty()) {

                if (build.getResult().isWorseThan(Result.SUCCESS)) {
                    scan(build, buildLog);
                    ProjectGraphAction.invalidateProjectGraphCache(build.getProject());
                    ComputerGraphAction.invalidateNodeGraphCache(build.getBuiltOn());
                } else if (PluginImpl.getInstance().getKnowledgeBase().isSuccessfulLoggingEnabled()) {
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
     * @param buildLog log to write information to.
     */
    public static void scan(AbstractBuild build, PrintStream buildLog) {
        try {
            Collection<FailureCause> causes = PluginImpl.getInstance().getKnowledgeBase().getCauses();
            List<FoundFailureCause> foundCauseListToLog = findCauses(causes, build, buildLog);
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
                foundCauseList.addAll(findFailedTests(build, buildLog));
            } else {
                foundCauseList = foundCauseListToLog;
            }

            FailureCauseBuildAction buildAction = new FailureCauseBuildAction(foundCauseList);
            buildAction.setBuild(build);
            build.addAction(buildAction);
            final FailureCauseDisplayData data = buildAction.getFailureCauseDisplayData();
            List<FailureCauseDisplayData> downstreamFailureCauses = data.getDownstreamFailureCauses();

            if (!downstreamFailureCauses.isEmpty()) {
                buildLog.println("[BFA] Found downstream Failure causes ...");
                for (FailureCauseDisplayData displayData : downstreamFailureCauses) {
                  FailureCauseDisplayData.Links links = displayData.getLinks();
                  buildLog.println("[BFA] See " + links.getProjectDisplayName() + links.getBuildDisplayName());
                  List<FoundFailureCause> failureCauses = displayData.getFoundFailureCauses();
                  for (FoundFailureCause foundCause : failureCauses) {
                    buildLog.println("[BFA] "
                                 + foundCause.getName() + " from category "
                                 + foundCause.getCategories().get(0));
                    }
                }
            }
            StatisticsLogger.getInstance().log(build, foundCauseListToLog);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not scan build " + build, e);
        }
    }

    /**
     * Finds the failure causes for this build.
     *
     * @param causes   the list of possible causes.
     * @param build    the build to analyze.
     * @param buildLog the build log.
     * @return a list of found failure causes.
     */
    private static List<FoundFailureCause> findCauses(final Collection<FailureCause> causes,
                                                      final AbstractBuild build, final PrintStream buildLog) {
        final List<FoundFailureCause> foundFailureCauseList =
                Collections.synchronizedList(new LinkedList<FoundFailureCause>());
        long start = System.currentTimeMillis();
        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.
                newFixedThreadPool(PluginImpl.getInstance().getNrOfScanThreads());
        buildLog.println("[BFA] Scanning build for known causes...");
        for (final FailureCause cause : causes) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("BFA-scanner-"
                                                           + build.getFullDisplayName() + ": "
                                                           + cause.getName());
                    List<FoundIndication> foundIndications = findIndications(cause, build, buildLog);
                    if (!foundIndications.isEmpty()) {
                        FoundFailureCause foundFailureCause = new FoundFailureCause(cause, foundIndications);
                        foundFailureCauseList.add(foundFailureCause);
                    }
                }
            });
        }
        executor.shutdown();
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                buildLog.print('.');
            }
        } catch (InterruptedException e) {
            logger.log(Level.FINE,
                    "Got interrupted while waiting for scanner threads to finish for {0}",
                    build.getFullDisplayName());
            buildLog.println("[BFA] Interrupted.");
        }
        long time = System.currentTimeMillis() - start;
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[BFA] [{0}] {1}ms", new Object[]
                    {build.getFullDisplayName(),
                            String.valueOf(time), });
        }
        if (!foundFailureCauseList.isEmpty()) {
            buildLog.println("[BFA] Found failure cause(s):");
            for (FoundFailureCause foundCause : foundFailureCauseList) {
                if (foundCause.getCategories() == null) {
                    buildLog.println("[BFA] " + foundCause.getName());
                } else {
                    buildLog.println("[BFA] "
                                 + foundCause.getName() + " from category "
                                 + foundCause.getCategories().get(0));
                }
            }

        } else {
            buildLog.println("[BFA] No failure causes found");
        }
        buildLog.println("[BFA] Done. " + TimeUnit.MILLISECONDS.toSeconds(time) + "s");
        return foundFailureCauseList;
    }

    /**
     * Finds the indications of a failure cause.
     *
     * @param cause    the cause to find indications for.
     * @param build    the build to analyze.
     * @param buildLog the build log.
     * @return a list of found indications for a cause.
     */
    private static List<FoundIndication> findIndications(FailureCause cause, AbstractBuild build, PrintStream buildLog) {
        long start = System.currentTimeMillis();
        List<Indication> indicationList = cause.getIndications();
        List<FoundIndication> foundIndicationList = new LinkedList<FoundIndication>();
        for (Indication indication : indicationList) {
            Thread.currentThread().setName("BFA-scanner-"
                                                   + build.getFullDisplayName() + ": "
                                                   + cause.getName() + "-"
                                                   + indication.getUserProvidedExpression());
            FoundIndication foundIndication = findIndication(indication, build, buildLog);
            if (foundIndication != null) {
                foundIndicationList.add(foundIndication);
            }
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[BFA] [{0}] [{1}] {2}ms", new Object[]
                    {build.getFullDisplayName(),
                            cause.getName(),
                            String.valueOf(System.currentTimeMillis() - start), });
        }
        return foundIndicationList;
    }

    /**
     * Finds out if this indication matches the build.
     *
     * @param indication the indication to look for.
     * @param build      the build to analyze.
     * @param buildLog   the build log.
     * @return an indication if one is found, null otherwise.
     */
    private static FoundIndication findIndication(Indication indication, AbstractBuild build, PrintStream buildLog) {
        FailureReader failureReader = indication.getReader();
        return failureReader.scan(build, buildLog);
    }

    /**
     * Finds the failed tests reported by this build
     *
     * @param build    the build to analyze.
     * @param buildLog the build log.
     * @return a list of found failure causes based on the test results
     */
    private static List<FoundFailureCause> findFailedTests(final AbstractBuild build, final PrintStream buildLog) {
        final List<FoundFailureCause> failedTestList =
            Collections.synchronizedList(new LinkedList<FoundFailureCause>());
        final List<AbstractTestResultAction> testActions =
            build.getActions(AbstractTestResultAction.class);

        for (AbstractTestResultAction testAction : testActions) {
            List<? extends TestResult> failedTests = testAction.getFailedTests();
            for (TestResult test : failedTests) {
                buildLog.println("[BFA] Found failed test case: " + test.getName());
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
