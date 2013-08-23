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

/* 
 * Displays a page to show graphs. Displays links for changing time-period
 * (today/month/all) and whether aborted builds should be showed or not. 
 * It assumes that the following variables are defined and accessible: 
 * - titleVar : The header-title
 * - sidePanelContext : The context for the sidepanel (e.g. CauseManagement.getOwner())
 * - imageSrcsArr : A vector of image-paths (src)
*/

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def j = namespace(lib.JenkinsTagLib)

def management = CauseManagement.getInstance();
def bgImageUrl = PluginImpl.getFullImageUrl("256x256", "information.png");
def timePeriod = request.getParameter("time");
if(timePeriod == null || !timePeriod.matches("today|month|all")) {
  timePeriod = "today";
}
def showAborted = request.getParameter("showAborted");
if(showAborted == null || !showAborted.matches("1|0")) {
  showAborted = 1;
}
def switchAborted = 0;
def switchAbortedText = "Hide aborted builds";
if(showAborted == "0") {
  switchAborted = 1;
  switchAbortedText = "Show aborted builds";
}
def trailingImageUrlParams = "&time=" + timePeriod + "&showAborted=" + showAborted;

l.layout(permission: PluginImpl.UPDATE_PERMISSION) {
  l.header() {
    style(type: "text/css") {
      raw("div.bfaSwitchLinks { margin: 10px; font-size: larger; }")
      raw("a#" + timePeriod + "-link, a#aborted-" + showAborted + " { text-decoration: underline; }")
      raw("div.bfaSwitchLinks a { margin: 5px 0; padding: 5px; text-decoration: none; color: #204a87; } ")
    }
  }

  l.side_panel() {
    if (!management.isUnderTest()) {
      include(my.owner, "sidepanel.jelly")
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

    h1(_(my.graphsPageTitle))
    
    div(class: "bfaSwitchLinks") {
      div(style: "float: right;") {
        span(style: "font-weight: bold; margin: 5px;") {
          text(_("Aborted builds"))
        }
        raw("<br />")
        a(href: "?time=" + timePeriod + "&showAborted=1",
            id: "aborted-1") {
          text(_("Show"))
        }
        a(href: "?time=" + timePeriod + "&showAborted=0",
            id: "aborted-0") {
          text(_("Hide"))
        }
      }
  
      div(style: "padding: 10px;") {
        span(style: "font-weight: bold; margin: 5px;") {
          text(_("Time period"))
        }
        div() {
          a(href: "?time=today&showAborted=" + showAborted,
            id: "today-link") {
            text(_("Today"))
          }
          a(href: "?time=month&showAborted=" + showAborted,
             id: "month-link") {
             text(_("Month"))
          }
          a(href: "?time=all&showAborted=" + showAborted,
             id: "all-link") {
             text(_("All"))
          }
        }
      }
    }
    
    div(style: "text-align: center;") {
      for(imgSrc in my.graphNumbers) {
        img(src: "graph/png?which=" + imgSrc + trailingImageUrlParams) {}
      }
    }
  }
}