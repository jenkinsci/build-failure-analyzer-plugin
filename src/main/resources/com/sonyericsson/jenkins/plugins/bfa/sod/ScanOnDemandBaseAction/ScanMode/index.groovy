/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
 * Copyright (c) 2016, CloudBees, Inc.
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

import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandBaseAction

def l = namespace(lib.LayoutTagLib)
def st = namespace("jelly:stapler")
def f = namespace(lib.FormTagLib)

ScanOnDemandBaseAction.ScanMode mode = my;
mode.setAsDefault()

l.layout(title: _("Failure Scan Options"), norefresh: true) {
    l.header {
        mode.parent.checkPermission()
        style(type: "text/css", """
            tr.disablehover:hover {
            background-color: white;
        }
""")

    }
    st.include(it: mode.parent.project, page: "sidepanel.jelly")
    l.'main-panel' {
        st.adjunct(includes: "com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandBaseAction.ScanMode.resource")
        f.form {
            f.section(title: _("Select the option and scan the builds")) {
                ScanOnDemandBaseAction.ScanMode.all().each { ScanOnDemandBaseAction.ScanMode option ->
                    f.entry(field: option.urlName, class: "bfa-scan-mode-build-type-radio-entry",
                            help: "/plugin/build-failure-analyzer/help/sod/${option.urlName}.html") {
                        span(class: "bfa-entry-data-holder", "data-root-url": "${rootURL}", "data-option-full-url": "${option.getFullUrl()}")
                        f.radio(name: "buildType",
                                value: option.urlName,
                                checked: option == mode,
                                title: option.displayName)
                    }
                }
            }
        }
        if (!mode.hasAnyRun(mode.parent.project)) {
            f.section(title: _("No build found")) {
                f.block {
                    table(width: "100%", border: "0", cellpadding: "2", cellspacing: "0",
                            class: "pane bigtable", style: "margin-top: 0") {
                        tr {
                            td()
                            td {
                                strong(_("No build found to scan"))
                            }
                        }
                    }
                }
            }
        } else {
            f.form(method: "POST", action: "performScan") {
                f.section {
                    f.block {
                        f.submit(value: _("Scan"))
                    }
                }
            }
        }
    }
}