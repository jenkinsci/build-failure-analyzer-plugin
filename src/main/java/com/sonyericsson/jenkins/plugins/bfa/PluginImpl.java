/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.Hudson;

/**
 * The main thing.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImpl extends Plugin {

    private static String staticResourcesBase = null;

    /**
     * Returns the base relative URI for static resources packaged in webapp.
     *
     * @return the base URI.
     */
    public static String getStaticResourcesBase() {
        if (staticResourcesBase == null) {
            PluginManager pluginManager = Hudson.getInstance().getPluginManager();
            if (pluginManager != null) {
                PluginWrapper wrapper = pluginManager.getPlugin(PluginImpl.class);
                if (wrapper != null) {
                    staticResourcesBase = "/plugin/" + wrapper.getShortName();
                }
            }
            //Did we really find it?
            if (staticResourcesBase == null) {
                //This is not the preferred way since the module name could change,
                //But in some unit test cases we cannot reach the plug-in info.
                return "/plugin/build-failure-analyzer";
            }
        }
        return staticResourcesBase;
    }

    /**
     * Returns the base relative URI for static images packaged in webapp.
     *
     * @return the images directory.
     *
     * @see #getStaticResourcesBase()
     */
    public static String getStaticImagesBase() {
        return getStaticResourcesBase() + "/images";
    }

    /**
     * Returns the singleton instance.
     *
     * @return the one.
     */
    public static PluginImpl getInstance() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
    }
}
