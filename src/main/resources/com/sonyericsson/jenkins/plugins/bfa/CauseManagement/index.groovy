/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications Inc. All rights reserved.
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
package com.sonyericsson.jenkins.plugins.bfa.CauseManagement
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl
import jenkins.model.Jenkins

import java.text.DateFormat

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def j = namespace(lib.JenkinsTagLib)

// Check permission manually. Using permissions layout() from groovy breaks view.jelly.
// Long story short, view.jelly interprets the exception text as jelly tags, which leads
// to a JellyTagException (internal server error).
Jenkins.get().checkPermission(PluginImpl.VIEW_PERMISSION)
l.layout(norefresh: true) {
  l.header(title: _("Failure Cause Management - Confirm Remove"))

  def management = CauseManagement.getInstance();

  l.side_panel() {
    if (!management.isUnderTest()) {
      include(management.getOwner(), "sidepanel.jelly")
    }
  }

  l.main_panel() {
    def appBarTitle = "List of Failure Causes"

    if (h.hasPermission(PluginImpl.UPDATE_PERMISSION)) {
      appBarTitle = "Update Failure Causes"
    }

    def shallowCauses = management.getShallowCauses()
    if (management.isError(request2)) {
      div(class: "error", id: "errorMessage") {
        text(management.getErrorMessage(request2))
      }
    }

    def removedCause = request2.getSession(true).getAttribute(CauseManagement.SESSION_REMOVED_FAILURE_CAUSE)
    if ( removedCause != null) {
      div(class: "info", style: "margin-top: 10px; margin-bottom: 10px") {
        text(_("Removed", removedCause.getName()))
      }
      request2.getSession().removeAttribute(CauseManagement.SESSION_REMOVED_FAILURE_CAUSE);
    }

    l.app_bar(title: appBarTitle) {
      //The New Cause link
      if (h.hasPermission(PluginImpl.UPDATE_PERMISSION)) {
         a(class: "jenkins-button jenkins-button--primary", href: "new") {
          l.icon(src:"symbol-add")
          text(_("Create new"))
        }
      }
    }

    //One time check so we don't do it for every iteration below
    def canRemove = Jenkins.getInstance().hasPermission(PluginImpl.REMOVE_PERMISSION)

    //Main FailureCauses table
    table(class: "jenkins-table sortable", id: "failureCausesTable") {
      thead {
        th{text(_("Name"))}
        th{text(_("Categories"))}
        th{text(_("Description"))}
        th{text(_("Comment"))}
        th{text(_("Modified"))}
        if (PluginImpl.getInstance().getKnowledgeBase().isEnableStatistics()) {
            th{text(_("Last seen"))}
        }
        th{text(" ")}
      }
      tbody {
        shallowCauses.each{ cause ->
          tr {
            td{
              if (h.hasPermission(PluginImpl.UPDATE_PERMISSION)) {
                a(href: cause.getId()) { text(cause.getName()) }
              } else {
                text(cause.getName())
              }
            }
            td{
              text(cause.getCategoriesAsString())
            }
            td{
              raw(app.markupFormatter.translate(cause.getDescription()))
            }
            td{
              text(cause.getComment())
            }
            td{
              def lastModified = cause.getLatestModification();
              if (lastModified != null) {
                def lastModifiedString = DateFormat.getDateTimeInstance(
                      DateFormat.SHORT, DateFormat.SHORT).format(lastModified.getTime());
                def user = lastModified.getUser();
                if (user == null) {
                  user = "unknown";
                }
                text(_("ModifiedBy", lastModifiedString, user))
              }
            }
            if (PluginImpl.getInstance().getKnowledgeBase().isEnableStatistics()) {
              def lastOccurred = cause.getAndInitiateLastOccurred();
              def lastOccurredString = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT).format(lastOccurred)
              if (lastOccurred == new Date(0)) {
                  lastOccurredString = "Never";
              }
              td(data: lastOccurred){
                  text(lastOccurredString)
              }
            }
            td {
              if (canRemove) {
                 a(class: "jenkins-button jenkins-!-destructive-color", href:"remove?id=" + cause.getId()) {
                  l.icon(src:"symbol-trash")
                  text(_("Remove"))
                }
              }
            }
          }
        }
      }
    }
  }
}

