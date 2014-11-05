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

package com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl

def f = namespace(lib.FormTagLib)
def j = namespace(lib.JenkinsTagLib)
def l = namespace(lib.LayoutTagLib)

index = 1

tr {
    td {
        img(width: "48", height: "48", src: my.getImageUrl(), style: "margin-right:1em;")
    }
    td(style: "vertical-align: middle;") {
        h2(_("Identified problems"))
    }
}

displayData(my.getFailureCauseDisplayData(), [], 0)

def displayData(failureCauseDisplayData, linkTree, indent) {

    if (failureCauseDisplayData.getFoundFailureCauses().empty && failureCauseDisplayData.getDownstreamFailureCauses().empty) {

        if (indent > 0) {
            displayLinkTree(linkTree)
        }

        tr {
            td {}
            td {
                h4(style: "margin-left: 10px;") {
                    text(_("No identified problem"))
                }
                h4(style: "margin-left: 10px; font-weight: normal") {
                    text(_(PluginImpl.getInstance().noCausesMessage))
                }
            }
        }
    }


    if (!failureCauseDisplayData.getFoundFailureCauses().empty && indent > 0) {
        displayLinkTree(linkTree)
    }

    failureCauseDisplayData.getFoundFailureCauses().each { cause ->
        displayCauses(cause, indent, failureCauseDisplayData.links)
    }
    failureCauseDisplayData.getDownstreamFailureCauses().each { subFailureCauseDisplayData ->
        linkTree.add(subFailureCauseDisplayData.links)
        displayData(subFailureCauseDisplayData, linkTree, indent + 1)
        linkTree.pop()
    }
}

def displayLinkTree(linkTree) {
    tr {
        td {}
        td {
            h3 {
                text(_("Subproject build: "))
                linkTree.eachWithIndex { link, i ->
                    if (link?.buildUrl != null) {
                        if (i > 0) {
                            text(" / ")
                        }
                        a(href: "${rootURL}/${link.projectUrl}", class: "model-link") {
                            text(link.projectDisplayName + " ")
                        }
                        text(" (")
                        a(href: "${rootURL}/${link.buildUrl}", class: "model-link") {
                            text(link.buildDisplayName)
                        }
                        text(") ")
                    }
                }
            }
        }
    }
}

def displayCauses(cause, indent, links) {

    tr {
        td {}
        td {
            h3(style: "margin-left: 20px;") {
                text(cause.name)
                br {}
                br {}
                b(style: "font-weight: normal") {
                    raw(app.markupFormatter.translate(cause.description))
                }
                br {}
                cause.getIndications().each { indication ->
                    if (links?.buildUrl != null) {
                        a(href: "${rootURL}/${links.buildUrl}" + "consoleFull#" + indication.matchingHash + cause.id
                                , class: "model-link") {
                            text(_("Indication") + " " + (index++))
                        }
                        text(" ")
                    }
                }
                br {}
            }
        }
    }
}
