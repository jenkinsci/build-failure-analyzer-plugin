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
package com.sonyericsson.jenkins.plugins.bfa.model.FailureCause
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl
import hudson.Util

import java.text.DateFormat

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def j = namespace(lib.JenkinsTagLib)

l.layout(permission: PluginImpl.UPDATE_PERMISSION) {
  l.header(title: _("Failure Cause Management") + " " + my.getName())

  def management = my.getAncestorCauseManagement();

  context.setVariable("descriptor", my.getDescriptor());
  descriptor.setLastFailedBuildUrl();

  l.side_panel() {
    if (!management.isUnderTest()) {
      include(management.getOwner(), "sidepanel.jelly")
    }
  }

  l.main_panel() {
    l.app_bar(title:"Failure Cause") {
      if (Util.fixEmpty(my.getId()) != null) {
        a(class: "jenkins-button jenkins-!-destructive-color", href: "../remove?id=" + my.getId(), tooltip: _("Remove this cause")) {
          l.icon(src:"symbol-trash")
          text(_("Remove"))
        }
      }
    }

    f.form(action: "configSubmit", method: "POST", name: "causeForm", class: "jenkins-form") {
      f.invisibleEntry() {
        f.textbox(field: "id", value: my.getId())
      }
      f.entry(title: _("Name"), field: "name") {
        f.textbox(value: my.getName(), checkMethod: "post")
      }
      f.entry(title: _("Description"), field: "description") {
        f.textarea(value: my.getDescription(), checkMethod: "post")
      }
      f.entry(title: _("Comment"), field: "comment") {
        f.textarea(value: my.getComment())
      }
      f.entry(title: _("Categories"), field: "categories") {
        f.textbox(value: my.getCategoriesAsString(), autoCompleteDelimChar: " ")
      }
      f.section(title: _("Indications")) {
        f.block {
          f.hetero_list(
                  name: "indications",
                  hasHeader: true,
                  descriptors: management.getIndicationDescriptors(),
                  items: my.getIndications(),
                  addCaption: _("Add Indication"),
                  deleteCaption: _("Delete Indication"))
        }
      }
      f.section(title: _("Modification history")) {
        def history = my.getAndInitiateModifications();
        f.block {
          if (history != null) {
            ul(id: "modifications") {
              history.each{ entry ->
                def dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                        DateFormat.SHORT).format(entry.getTime());
                li {text(_("ModifiedBy", dateFormat,
                        entry.getUser() == null ? "unknown": entry.getUser()))}
              }
            }
          }
        }
      }
      f.block {
        div(style: "margin-top: 10px")
        f.submit(value: _("Save"))
      }
    }
  }
}
