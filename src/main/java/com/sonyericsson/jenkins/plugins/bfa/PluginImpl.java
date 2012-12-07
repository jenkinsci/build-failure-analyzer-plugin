/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
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

import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty;
import hudson.ExtensionList;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main thing.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImpl extends Plugin {

    private static final Logger logger = Logger.getLogger(PluginImpl.class.getName());

    /**
     * Convenience constant for the 24x24 icon size. used for {@link #getImageUrl(String, String)}.
     */
    public static final String DEFAULT_ICON_SIZE = "24x24";

    /**
     * Convenience constant for the default icon size. used for {@link #getImageUrl(String, String)}.
     */
    public static final String DEFAULT_ICON_NAME = "information.png";

    /**
     * Default number of concurrent scan threads.
     */
    public static final int DEFAULT_NR_OF_SCAN_THREADS = 3;

    /**
     * The permission group for all permissions related to this plugin.
     */
    public static final PermissionGroup PERMISSION_GROUP =
            new PermissionGroup(PluginImpl.class, Messages._PermissionGroup_Title());

    /**
     * Permission to update the causes. E.e. Access {@link CauseManagement}.
     */
    public static final Permission UPDATE_PERMISSION =
            new Permission(PERMISSION_GROUP, "UpdateCauses",
                    Messages._PermissionUpdate_Description(), Hudson.ADMINISTER);

    /**
     * Permission to remove causes.
     */
    public static final Permission REMOVE_PERMISSION =
            new Permission(PERMISSION_GROUP, "RemoveCause",
                    Messages._PermissionRemove_Description(), Hudson.ADMINISTER);

    private static final String DEFAULT_NO_CAUSES_MESSAGE = "No problems were identified. "
            + "If you know why this problem occurred, please add a suitable Cause for it.";

    private static String staticResourcesBase = null;

    /**
     * Minimum allowed value for {@link #nrOfScanThreads}.
     */
    protected static final int MINIMUM_NR_OF_SCAN_THREADS = 1;

    private String noCausesMessage;

    private Boolean globalEnabled;

    private Boolean gerritTriggerEnabled;

    private transient CopyOnWriteList<FailureCause> causes;

    private KnowledgeBase knowledgeBase;

    private int nrOfScanThreads;


    @Override
    public void start() throws Exception {
        super.start();
        logger.finer("[BFA] Starting...");
        load();
        if (noCausesMessage == null) {
            noCausesMessage = DEFAULT_NO_CAUSES_MESSAGE;
        }
        if (nrOfScanThreads < 1) {
            nrOfScanThreads = DEFAULT_NR_OF_SCAN_THREADS;
        }
        if (knowledgeBase == null) {
            if (causes == null) {
                knowledgeBase = new LocalFileKnowledgeBase();
            } else {
                //Migrate old data.
                knowledgeBase = new LocalFileKnowledgeBase(causes);
                //No reason to keep it in memory right?
                causes = null;
            }
        }
        try {
            knowledgeBase.start();
            logger.fine("[BFA] Started!");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not initialize the knowledge base: ", e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        knowledgeBase.stop();
    }

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
     * Provides a Jenkins relative url to a plugin internal image.
     *
     * @param size the size of the image (the sub directory of images).
     * @param name the name of the image file.
     * @return a URL to the image.
     */
    public static String getImageUrl(String size, String name) {
        return getStaticImagesBase() + "/" + size + "/" + name;
    }

    /**
     * Get the full url to an image, including rootUrl and context path.
     *
     * @param size the size of the image (the sub directory of images).
     * @param name the name of the image file.
     * @return a URL to the image.
     */
    public static String getFullImageUrl(String size, String name) {
        return Hudson.getInstance().getRootUrlFromRequest() + getStaticImagesBase() 
            + "/" + size + "/" + name;
    }

    /**
     * Provides a Jenkins relative url to a plugin internal image of {@link #DEFAULT_ICON_SIZE} size.
     *
     * @param name the name of the image.
     * @return a URL to the image.
     *
     * @see #getImageUrl(String, String)
     */
    public static String getImageUrl(String name) {
        return getImageUrl(DEFAULT_ICON_SIZE, name);
    }

    /**
     * The default icon to be used throughout this plugin.
     *
     * @return the relative URL to the image.
     *
     * @see #getImageUrl(String)
     * @see #getImageUrl(String, String)
     */
    public static String getDefaultIcon() {
        return getImageUrl(DEFAULT_ICON_NAME);
    }

    /**
     * Returns the singleton instance.
     *
     * @return the one.
     */
    public static PluginImpl getInstance() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
    }

    /**
     * Getter for the no causes message.
     *
     * @return the message.
     */
    public String getNoCausesMessage() {
        return noCausesMessage;
    }

    /**
     * If this feature is enabled or not. When on all unsuccessful builds will be scanned. None when off.
     *
     * @return true if on.
     */
    public boolean isGlobalEnabled() {
        if (globalEnabled == null) {
            return true;
        } else {
            return globalEnabled;
        }
    }

    /**
     * Sets if this feature is enabled or not. When on all unsuccessful builds will be scanned. None when off.
     *
     * @param globalEnabled on or off. null == on.
     */
    public void setGlobalEnabled(Boolean globalEnabled) {
        this.globalEnabled = globalEnabled;
    }

    /**
     * Send notifications to Gerrit-Trigger-plugin.
     *
     * @return true if on.
     */
    public boolean isGerritTriggerEnabled() {
        if (gerritTriggerEnabled == null) {
            return true;
        } else {
            return gerritTriggerEnabled;
        }
    }

    /**
     * Sets if this feature is enabled or not. When on, cause descriptions will be forwarded to Gerrit-Trigger-Plugin.
     *
     * @param gerritTriggerEnabled on or off. null == on.
     */
    public void setGerritTriggerEnabled(Boolean gerritTriggerEnabled) {
        this.gerritTriggerEnabled = gerritTriggerEnabled;
    }

    /**
     * The number of threads to have in the pool for each build. Used by the {@link BuildFailureScanner}.
     * Will return nothing less than {@link #MINIMUM_NR_OF_SCAN_THREADS}.
     *
     * @return the number of scan threads.
     */
    public int getNrOfScanThreads() {
        if (nrOfScanThreads < MINIMUM_NR_OF_SCAN_THREADS) {
            nrOfScanThreads = DEFAULT_NR_OF_SCAN_THREADS;
        }
        return nrOfScanThreads;
    }


    /**
     * The number of threads to have in the pool for each build. Used by the {@link BuildFailureScanner}.
     * Will throw an {@link IllegalArgumentException} if the parameter is less than {@link #MINIMUM_NR_OF_SCAN_THREADS}.
     *
     * @param nrOfScanThreads the number of scan threads.
     */
    public void setNrOfScanThreads(int nrOfScanThreads) {
        if (nrOfScanThreads < MINIMUM_NR_OF_SCAN_THREADS) {
            throw new IllegalArgumentException("Minimum nrOfScanThreads is " + MINIMUM_NR_OF_SCAN_THREADS);
        }
        this.nrOfScanThreads = nrOfScanThreads;
    }

    /**
     * Checks if the specified build should be scanned or not. Determined by {@link #isGlobalEnabled()} and if the
     * build's project has {@link com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty#isDoNotScan()}.
     *
     * @param build the build
     * @return true if it should be scanned.
     */
    public static boolean shouldScan(AbstractBuild build) {
        if (getInstance().isGlobalEnabled()) {
            AbstractProject project = build.getProject();
            ScannerJobProperty property = (ScannerJobProperty)project.getProperty(ScannerJobProperty.class);
            if (property != null) {
                return !property.isDoNotScan();
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * The knowledge base containing all causes.
     *
     * @return all the base.
     */
    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    /**
     * Convenience method to reach the list from jelly.
     *
     * @return the list of registered KnowledgeBaseDescriptors
     */
    public ExtensionList<KnowledgeBase.KnowledgeBaseDescriptor> getKnowledgeBaseDescriptors() {
        return KnowledgeBase.KnowledgeBaseDescriptor.all();
    }

    /**
     * Gets the KnowledgeBaseDescriptor that matches the name descString.
     *
     * @param descString either name of a KnowledgeBaseDescriptor or the fully qualified name.
     * @return The matching KnowledgeBaseDescriptor or null if none is found.
     */
    public KnowledgeBase.KnowledgeBaseDescriptor getKnowledgeBaseDescriptor(String descString) {
        for (KnowledgeBase.KnowledgeBaseDescriptor desc : getKnowledgeBaseDescriptors()) {
            if (desc.getClass().toString().contains(descString)) {
                return desc;
            }
        }
        return null;
    }

    @Override
    public void configure(StaplerRequest req, JSONObject o) throws Descriptor.FormException, IOException {
        noCausesMessage = o.getString("noCausesMessage");
        globalEnabled = o.getBoolean("globalEnabled");
        gerritTriggerEnabled = o.getBoolean("gerritTriggerEnabled");
        int scanThreads = o.getInt("nrOfScanThreads");
        if (scanThreads < MINIMUM_NR_OF_SCAN_THREADS) {
            nrOfScanThreads = DEFAULT_NR_OF_SCAN_THREADS;
        } else {
            nrOfScanThreads = scanThreads;
        }
        KnowledgeBase base = req.bindJSON(KnowledgeBase.class, o.getJSONObject("knowledgeBase"));
        if (base != null && !knowledgeBase.equals(base)) {
            try {
                base.start();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not start new knowledge base, reverting ", e);
                save();
                return;
            }
            if (o.getBoolean("convertOldKb")) {
                try {
                    base.convertFrom(knowledgeBase);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not convert knowledge base ", e);
                }
            }
            knowledgeBase.stop();
            knowledgeBase = base;
        }
        save();
    }
}
