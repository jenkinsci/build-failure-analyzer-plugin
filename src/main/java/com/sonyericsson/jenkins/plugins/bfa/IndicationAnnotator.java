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
package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotates the build log so that we can create links to it and mark found indications.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class IndicationAnnotator extends ConsoleAnnotator<Object> {

    private Map<String, AnnotationHelper> helperMap;


    /**
     * Standard constructor.
     *
     * @param foundFailureCauses the {@link FoundFailureCause}s to add annotation for.
     */
    public IndicationAnnotator(List<FoundFailureCause> foundFailureCauses) {
        helperMap = new HashMap<String, AnnotationHelper>();
        for (FoundFailureCause foundFailureCause : foundFailureCauses) {
            addToHelperMap(foundFailureCause);
        }
    }

    /**
     * Adds the matching and focus lines to the helper map, to ease annotating later.
     *
     * @param cause the {@link FoundFailureCause}} to add lines for.
     */
    private void addToHelperMap(FoundFailureCause cause) {
        for (FoundIndication indication : cause.getIndications()) {
            String matchingString = indication.getMatchingString();
            if (matchingString != null && !matchingString.isEmpty()) {
                AnnotationHelper matchingHelper = helperMap.get(matchingString);
                if (matchingHelper == null) {
                    matchingHelper = new AnnotationHelper();
                    matchingHelper.addAfter("</span>");
                }
                matchingHelper.addTitle(cause.getName());
                matchingHelper.addFocus(indication.getMatchingHash() + cause.getId());
                matchingHelper.addAfter("</span>");
                helperMap.put(matchingString, matchingHelper);
            }
        }
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text) {
        AnnotationHelper match = helperMap.get(text.getText().trim());
        if (match != null) {
            text.wrapBy(match.getBefore(), match.getAfter());
        }
        return this;
    }
}
