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

package com.sonyericsson.jenkins.plugins.bfa.test.utils;

import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class DifferentKnowledgeBase extends LocalFileKnowledgeBase {

    private String someString;

    /**
     * Standard Databound constructor.
     *
     * @param someString the string.
     */
    @DataBoundConstructor
    public DifferentKnowledgeBase(String someString) {
        this.someString = someString;
    }

    /**
     * Standard constructor.
     *
     * @param initial the initial db, can be null.
     */
    public DifferentKnowledgeBase(List<FailureCause> initial) {
        if (initial != null) {
            for (FailureCause c : initial) {
                saveCause(c);
            }
        }
    }

    @Override
    public boolean equals(KnowledgeBase oldKnowledgeBase) {
        return super.equals(oldKnowledgeBase)
                && ((DifferentKnowledgeBase)oldKnowledgeBase).someString.equals(someString);
    }

    //CS IGNORE AvoidInlineConditionals FOR NEXT 29 LINES. REASON: Auto generated code.

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        DifferentKnowledgeBase that = (DifferentKnowledgeBase)o;

        if (someString != null ? !someString.equals(that.someString) : that.someString != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        //CS IGNORE MagicNumber FOR NEXT 3 LINES. REASON: Auto generated code.
        int result = super.hashCode();
        result = 31 * result + (someString != null ? someString.hashCode() : 0);
        return result;
    }

    /**
     * A configurable string.
     *
     * @return some string.
     */
    public String getSomeString() {
        return someString;
    }

    /**
     * A configurable string.
     *
     * @param someString some string.
     */
    public void setSomeString(String someString) {
        this.someString = someString;
    }

    @Override
    public Descriptor<KnowledgeBase> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DifferentKnowledgeBaseDescriptor.class);
    }

    /**
     * Descriptor for {@link DifferentKnowledgeBase}.
     */
    @Extension
    public static class DifferentKnowledgeBaseDescriptor extends KnowledgeBaseDescriptor {

        @Override
        public String getDisplayName() {
            return "A Different One";
        }
    }
}
