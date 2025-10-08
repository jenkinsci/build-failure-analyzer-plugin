/*
 * The MIT License
 *
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
package com.sonyericsson.jenkins.plugins.bfa.CauseManagement;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import hudson.Functions

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def j = namespace(lib.JenkinsTagLib)
def st = namespace("jelly:stapler")

l.layout(permission: PluginImpl.REMOVE_PERMISSION) {
  l.header(title: _("Failure Cause Management - Confirm Remove"))

  def management = CauseManagement.getInstance();
  def causeId = request2.getParameter("id");
  def cause = null;
  if (causeId != null && !causeId.isEmpty()) {
    cause = PluginImpl.getInstance().getKnowledgeBase().getCause(causeId);
  }

  l.side_panel() {
    if (!management.isUnderTest()) {
      include(management.getOwner(), "sidepanel.jelly")
    }
  }

  l.main_panel() {
    h1(_("Failure Cause - Confirm Remove"))
    div(style: "width: 70%") {
      st.adjunct(includes: "com.sonyericsson.jenkins.plugins.bfa.CauseManagement.resource")
      if (cause != null) {
        form(method: "POST", action: "removeConfirm") {
          p(_("removeQuestion", cause.getName()))
          input(type: "hidden", value: cause.getId(), name: "id")
          raw("&nbsp;&nbsp;");
          f.submit(value: _("Yes"))
          raw("&nbsp;&nbsp;");
          button(type: "button", class: "jenkins-button bfa-cause-management-back-button", _("Back"))
        }
      } else {
        p(_("Not a valid cause id"))
        button(class: "jenkins-button bfa-cause-management-back-button", _("Back"))
      }

    }
  }
}
