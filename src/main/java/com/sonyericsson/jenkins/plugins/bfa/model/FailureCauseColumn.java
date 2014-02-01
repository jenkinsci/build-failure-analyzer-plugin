/*
 * The MIT License
 *
 * Copyright 2014 Vincent Latombe
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
