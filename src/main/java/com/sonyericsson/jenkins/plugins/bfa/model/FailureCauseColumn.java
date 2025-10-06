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
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.ListViewColumn;

import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A column that user can add to a view to display the failure cause of the last build.
 *
 * @author vlatombe
 *
 */
public class FailureCauseColumn extends ListViewColumn {

  /**
   * The descriptor for {@link FailureCauseColumn}.
   * @author vlatombe
   *
   */
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

  /**
   * The standard data-bound constructor.
   *
   * @param showText
   *          if true, will display the text of the failure cause next to the icon
   */
  @DataBoundConstructor
  public FailureCauseColumn(boolean showText) {
    this.showText = showText;
  }

  /**
   * @see FailureCauseBuildAction#getBadgeImageUrl()
   * @param job The given job we want the badge image url for
   * @return the image url
   * @deprecated plugin now uses icons. Please use {@link #getIconFileName(Job)} instead.
   */
  @Deprecated
  public String getBadgeImageUrl(Job job) {
    FailureCauseBuildAction action = findFailureCauseBuildAction(job);
    if (action == null) {
      return null;
    }
    return action.getBadgeImageUrl();
  }

  /**
   * @see FailureCauseBuildAction#getIconFileName()
   * @param job The given job we want the badge icon name for
   * @return the icon name
   */
  public String getIconFileName(Job job) {
    FailureCauseBuildAction action = findFailureCauseBuildAction(job);
    if (action == null) {
      return null;
    }
    return action.getIconFileName();
  }


  /**
   * @see FailureCauseBuildAction#getFoundFailureCauses()
   * @param job the job we want to retrieve actions for
   * @return the list of found failure causes
   */
  public List<FoundFailureCause> getFoundFailureCauses(Job job) {
    FailureCauseBuildAction action = findFailureCauseBuildAction(job);
    if (action == null) {
      return Collections.emptyList();
    }
    return action.getFoundFailureCauses();
  }

  /**
   * @return true if text should be displayed next to the failure cause icon
   */
  public boolean isShowText() {
    return showText;
  }

  /**
   * A helper method to retrieve the {@link FailureCauseBuildAction} from the given {@link Job}
   * @param job the given job
   * @return The {@link FailureCauseBuildAction} if it exists, otherwise null
   */
  private FailureCauseBuildAction findFailureCauseBuildAction(Job job) {
    if (job == null) {
      return null;
    }
    Run lastBuild = job.getLastBuild();
    if (lastBuild == null) {
      return null;
    }
    return lastBuild.getAction(FailureCauseBuildAction.class);
  }

}
