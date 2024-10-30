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

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.Extension;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleAnnotatorFactory;
import hudson.model.Run;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.List;

/**
 * Factory for creating a new {@link IndicationAnnotator} when the log should be annotated.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@Extension
public class IndicationAnnotatorFactory extends ConsoleAnnotatorFactory {

    @Override
    public ConsoleAnnotator newInstance(Object context) {
        StaplerRequest2 currentRequest = Stapler.getCurrentRequest2();
        if (currentRequest == null) {
            //Accessed through some other means than http, so lets assume it is not a human.
            return null;
        }
        Ancestor ancestor = currentRequest.findAncestor(Run.class);
        if (ancestor == null) {
            return null;
        }
        Object object = ancestor.getObject();
        Run build = (Run)object;
        FailureCauseBuildAction action = build.getAction(FailureCauseBuildAction.class);
        if (action == null) {
            return null;
        }
        List<FoundFailureCause> foundFailureCauses = action.getFoundFailureCauses();
        if (foundFailureCauses.isEmpty()) {
            return null;
        }
        return new IndicationAnnotator(foundFailureCauses);
    }
}
