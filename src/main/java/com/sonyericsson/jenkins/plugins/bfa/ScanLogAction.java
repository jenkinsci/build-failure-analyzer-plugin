package com.sonyericsson.jenkins.plugins.bfa;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jenkins.model.RunAction2;
import org.apache.commons.io.FileUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * The Action for adding a link to the analysis for each run.
 */
@ExportedBean
public class ScanLogAction implements RunAction2 {

    /**
     * Log file name.
     */
    public static final String FILE_NAME = "com.sonyericsson.jenkins.plugins.bfa.ScanLogAction.log";

    private transient Run run;

    private long startTime = System.currentTimeMillis();

    private Long endTime;

    private String exceptionMessage;

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getIconFileName() {
        return PluginImpl.getDefaultIcon();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.ScanLogAction_DisplayName();
    }

    /**
     * The run associated with this action, called by jelly.
     * @return the run
     */
    public Run getRun() {
        return run;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getUrlName() {
        return "failure-cause-scan-log";
    }

    /**
     * Log text for the analysis.
     * @return the log text, lines are separated by \n
     * @throws IOException if the log can't be found
     */
    public String getLogText() throws IOException {
        return FileUtils.readFileToString(new File(run.getRootDir(), FILE_NAME), StandardCharsets.UTF_8);
    }

    /**
     * The start time of the current scan.
     * @return time in milliseconds {@link System#currentTimeMillis}
     */
    @Exported
    public long getStartTime() {
        return startTime;
    }

    /**
     * The end time of the current scan.
     * @return time in milliseconds {@link System#currentTimeMillis}
     */
    @Exported
    public Long getEndTime() {
        return endTime;
    }

    /**
     * To call when the scan is finished.
     */
    @Restricted(NoExternalUse.class)
    protected void finished() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Get the exception message if any.
     * @return the first exception faced during scan
     */
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    /**
     * Set an exception.
     * @param exceptionMessage the exception message to set
     */
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }
}
