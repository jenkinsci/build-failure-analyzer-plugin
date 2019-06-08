package com.sonyericsson.jenkins.plugins.bfa;

import hudson.model.Action;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class ScanLogAction implements Action {

    public static final String FILE_NAME = "com.sonyericsson.jenkins.plugins.bfa.ScanLogAction.log";

    private final Run run;

    public ScanLogAction(Run run) {
        this.run = run;
    }

    @Nonnull
    @Override
    public String getIconFileName() {
        return PluginImpl.getDefaultIcon();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Failure Scan Log";
    }

    public Run getRun() {
        return run;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "failure-cause-management-log";
    }

    public String getLogText() throws IOException {
        return Files.lines(new File(run.getRootDir(), FILE_NAME).toPath())
                .collect(Collectors.joining("\n"));
    }
}
