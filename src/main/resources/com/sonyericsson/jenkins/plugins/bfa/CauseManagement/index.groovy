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

l.layout(permission: PluginImpl.UPDATE_PERMISSION) {
  l.header(title: _("Failure Cause Management - Confirm Remove"))

  def management = CauseManagement.getInstance();
  def bgImageUrl = PluginImpl.getFullImageUrl("256x256", "information.png");
  def newImageUrl = PluginImpl.getFullImageUrl("24x24", "newinformation.png");

  l.side_panel() {
    if (!management.isUnderTest()) {
      include(management.getOwner(), "sidepanel.jelly")
    }
  }

  l.main_panel() {
    div(style: "width: 256px; "
            + "height: 256px;"
            + "opacity:0.2;"
            + "right:-10px;"
            + "top: 50px;"
            + "position: absolute;"
            + "z-index: -100;"
            + "background-image: url(\'" + bgImageUrl + "');") {}

    h1(_("Update Failure Causes"))

    def shallowCauses = management.getShallowCauses()
    if (management.isError(request)) {
      div(class: "error", id: "errorMessage") {
        text(management.getErrorMessage(request))
      }
    }

    def removedCause = request.getSession(true).getAttribute(CauseManagement.SESSION_REMOVED_FAILURE_CAUSE)
    if ( removedCause != null) {
      div(class: "info", style: "margin-top: 10px; margin-bottom: 10px") {
        text(_("Removed", removedCause.getName()))
      }
      request.getSession().removeAttribute(CauseManagement.SESSION_REMOVED_FAILURE_CAUSE);
    }

    //The New Cause link
    div(style: "margin-top: 10px; margin-bottom: 10px; width: 90%;") {
      a(style:  "font-weight: bold; "
                + "font-size: larger; "
                + "padding-left: 30px; "
                + "min-height: 30px; "
                + "padding-top: 5px; "
                + "padding-bottom: 5px; "
                + "background-image: url( \'" + newImageUrl + "\'); "
                + "background-position: left center; "
                + "background-repeat: no-repeat;",
        href: "new",
        alt: _("New")) {text(_("Create new"))}

      if (PluginImpl.getInstance().isGraphsEnabled()) {
        a(style:  "font-weight: bold; "
                + "font-size: larger; "
                + "padding-top: 5px; "
                + "padding-bottom: 5px; "
                + "float: right;",
          href: "detailedgraphs",
          alt: _("Graphs/statistics")) {text(_("Graphs/Statistics"))}
      }
    }

    //One time check so we don't do it for every iteration below
    def canRemove = Jenkins.getInstance().hasPermission(PluginImpl.REMOVE_PERMISSION)

    //Main FailureCauses table
    table(cellpadding: "2", cellspacing: "0", border: "1", class: "sortable pane bigtable", width: "90%",
                   style: "width: 90%; white-space: normal", id: "failureCausesTable") {
      tr {
        th{text(_("Name"))}
        th{text(_("Categories"))}
        th{text(_("Description"))}
        th{text(_("Comment"))}
        th{text(_("Modified"))}
        if (PluginImpl.getInstance().getKnowledgeBase().isStatisticsEnabled()) {
            th{text(_("Last seen"))}
        }
        th{text(" ")}
      }

      shallowCauses.each{ cause ->
        tr {
          td{
            a(href: cause.getId()){ text(cause.getName())}
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
          if (PluginImpl.getInstance().getKnowledgeBase().isStatisticsEnabled()) {
            def lastOccurred = cause.getAndInitiateLastOccurred();
            def lastOccurredString = DateFormat.getDateTimeInstance(
                  DateFormat.SHORT, DateFormat.SHORT).format(lastOccurred)
            if (lastOccurred == new Date(0)) {
                lastOccurredString = "Never";
            }
            td{
                text(lastOccurredString)
            }
          }
          td {
            if (canRemove) {
              a(href: "remove?id=" + cause.getId(), title:_("Remove")) {
                img(src: imagesURL + "/16x16/edit-delete.png", alt: _("Remove"))
              }
            }
          }
        }
      }
    }
  }
}

