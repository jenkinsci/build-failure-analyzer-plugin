/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications AB. All rights reserved.
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
import com.sonyericsson.jenkins.plugins.bfa.CauseManagement;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;

/* 
 * Displays a page to show graphs. Displays links for changing time-period
 * (today/month/all) and whether aborted builds should be showed or not. 
*/

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def j = namespace(lib.JenkinsTagLib)

def management = CauseManagement.getInstance();
def bgImageUrl = PluginImpl.getFullImageUrl("256x256", "information.png");
def timePeriod = request.getParameter("time");
if (timePeriod == null || !timePeriod.matches("today|month|max")) {
  timePeriod = "today";
}
def showAborted = request.getParameter("showAborted");
if (showAborted == null || !showAborted.matches("1|0")) {
  showAborted = 0;
}

def linkBoxWidth = "50%";
def masterValue = "";
if (my.showMasterSwitch()) {
  masterValue = request.getParameter("allMasters");
  if (masterValue == null || !masterValue.matches("0|1")) {
    masterValue = "0";
  }
  linkBoxWidth = "33%";
}

def trailingImageUrlParams = "&time=" + timePeriod + "&showAborted=" + showAborted + "&allMasters=" + masterValue;

l.layout() {
  l.header() {
    style(type: "text/css") {
      raw("div.bfaSwitchLinks { margin: 20px 0; font-size: larger; }")
      raw("div.bfaSwitchLinks > div { float: left; width: " + linkBoxWidth + "; }")
      raw("a#" + timePeriod + "-link, a#aborted-" + showAborted + ", a#masters-" + masterValue + " { background-color: #dddddd; }")
      raw("div.bfaSwitchLinks a { padding: 2px 4px; text-decoration: none; color: #204a87; line-height: 140%;}")
      raw("div.bfaSwitchLinks span { font-weight: bold; }")
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
      div(style: "text-align: center;") {
        span() {
          text(_("Time period"))
        }
        div() {
          def trailingUrl = "&showAborted=" + showAborted + "&allMasters=" + masterValue;
          a(href: "?time=today" + trailingUrl,
            id: "today-link") {
            text(_("Today"))
          }
          a(href: "?time=month" + trailingUrl,
             id: "month-link") {
             text(_("Month"))
          }
          a(href: "?time=max" + trailingUrl,
             id: "max-link") {
             text(_("Max"))
          }
        }
      }

      if (my.showMasterSwitch()) {
        div(style: "text-align: center;") {
          span() {
            text(_("Masters"))
          }
          raw("<br />")
          a(href: "?time=" + timePeriod + "&showAborted=" + showAborted + "&allMasters=0",
              id: "masters-0") {
            text(_("This"))
          }
          raw("&nbsp;")
          a(href: "?time=" + timePeriod + "&showAborted=" + showAborted + "&allMasters=1",
              id: "masters-1") {
            text(_("All"))
          }
        }
      }

      div(style: "text-align: center;") {
        span() {
          text(_("Aborted builds"))
        }
        raw("<br />")
        a(href: "?time=" + timePeriod + "&showAborted=0&allMasters=" + masterValue,
            id: "aborted-0") {
          text(_("Hide"))
        }
        a(href: "?time=" + timePeriod + "&showAborted=1&allMasters=" + masterValue,
            id: "aborted-1") {
          text(_("Show"))
        }
      }

    }

    div() {
      for (graphType in my.graphTypes) {
        def imgSrc = graphType.getValue();
        div(style: "width: " + my.defaultGraphWidth + "px; height: "
                + my.defaultGraphHeight + "px; margin: 10px auto; border: 1px solid #000000;") {
          img(src: "graph/png?which=" + imgSrc + trailingImageUrlParams,
                lazymap: "graph/map?which=" + imgSrc + trailingImageUrlParams) {}
        }
      }
    }

    if (my.showGraphDelayText()) {
      div() {
        text(_("graph-cache-delay-info",
            com.sonyericsson.jenkins.plugins.bfa.graphs.GraphCache.expirationTime))
      }
    }

  }
}
