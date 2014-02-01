package com.sonyericsson.jenkins.plugins.bfa.model;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.ListViewColumn;

import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class FailureCauseColumn extends ListViewColumn {

  @Extension
  public static class DescriptorImpl extends ListViewColumnDescriptor {
    @Override
    public String getDisplayName() {
      return "Failure Cause";
    }

    @Override
    public boolean shownByDefault() {
      return false;
    }
  }

  private boolean showText;

  @DataBoundConstructor
  public FailureCauseColumn(boolean showText) {
    this.showText = showText;
  }

  public String getBadgeImageUrl(AbstractProject job) {
    FailureCauseBuildAction action = findFailureCauseBuildAction(job);
    if (action == null) {
      return null;
    }
    return action.getBadgeImageUrl();
  }

  public List<FoundFailureCause> getFoundFailureCauses(AbstractProject job) {
    FailureCauseBuildAction action = findFailureCauseBuildAction(job);
    if (action == null) {
      return Collections.emptyList();
    }
    return action.getFoundFailureCauses();
  }

  public boolean isShowText() {
    return showText;
  }

  public void setShowText(boolean showText) {
    this.showText = showText;
  }

  private FailureCauseBuildAction findFailureCauseBuildAction(AbstractProject job) {
    if (job == null) {
      return null;
    }
    Run lastBuild = job.getLastBuild();
    if (lastBuild == null) {
      return null;
    }
    FailureCauseBuildAction action = lastBuild.getAction(FailureCauseBuildAction.class);
    return action;
  }

}
