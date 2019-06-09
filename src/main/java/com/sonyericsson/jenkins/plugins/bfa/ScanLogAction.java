package com.sonyericsson.jenkins.plugins.bfa;

import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import jenkins.model.RunAction2;

/**
 * The Action for adding a link to the analysis for each run.
 */
public class ScanLogAction implements RunAction2 {

    /**
     * Log file name.
     */
    public static final String FILE_NAME = "com.sonyericsson.jenkins.plugins.bfa.ScanLogAction.log";

    private transient Run run;

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public String getIconFileName() {
        return PluginImpl.getDefaultIcon();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
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
    @Nonnull
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
        return Files.lines(new File(run.getRootDir(), FILE_NAME).toPath())
                .collect(Collectors.joining("\n"));
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
