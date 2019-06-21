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
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandQueue;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.init.Terminator;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.CopyOnWriteList;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * The main thing.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension
@Symbol("buildFailureAnalyzer")
public class PluginImpl extends GlobalConfiguration {

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
     * Default max size of log to be scanned ('0' disables check).
     */
    public static final int DEFAULT_MAX_LOG_SIZE = 0;

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

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
     * Permission to view the causes. E.e. Access {@link CauseManagement}.
     */
    public static final Permission VIEW_PERMISSION =
            new Permission(PERMISSION_GROUP, "ViewCauses",
                    Messages._PermissionView_Description(), UPDATE_PERMISSION);

    /**
     * Permission to remove causes.
     */
    public static final Permission REMOVE_PERMISSION =
            new Permission(PERMISSION_GROUP, "RemoveCause",
                    Messages._PermissionRemove_Description(), Hudson.ADMINISTER);

    private static final String DEFAULT_NO_CAUSES_MESSAGE = "No problems were identified. "
            + "If you know why this problem occurred, please add a suitable Cause for it.";

    /**
     * Minimum allowed value for {@link #nrOfScanThreads}.
     */
    protected static final int MINIMUM_NR_OF_SCAN_THREADS = 1;

    private String noCausesMessage;

    private Boolean globalEnabled;
    private boolean doNotAnalyzeAbortedJob;

    private Boolean gerritTriggerEnabled;

    private transient CopyOnWriteList<FailureCause> causes;

    private KnowledgeBase knowledgeBase;

    private int nrOfScanThreads;
    private int maxLogSize;

    private Boolean graphsEnabled;

    private Boolean testResultParsingEnabled;
    private String testResultCategories;

    /**
     * ScanOnDemandVariable instance.
     */
    private ScanOnDemandVariables sodVariables = new ScanOnDemandVariables();

    /**
     * Default constructor.
     */
    @DataBoundConstructor
    public PluginImpl() {
        load();
    }

    protected Object readResolve() {
        if (sodVariables == null) {
            this.sodVariables = new ScanOnDemandVariables();
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmlFile getConfigFile() {
        return new XmlFile(
                Jenkins.XSTREAM,
                new File(Jenkins.getInstance().getRootDir(), "build-failure-analyzer.xml")
        ); // for backward compatibility
    }

    /**
     * Starts the knowledge base.
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public void start() {
        logger.finer("[BFA] Starting...");
        if (noCausesMessage == null) {
            noCausesMessage = DEFAULT_NO_CAUSES_MESSAGE;
        }
        if (testResultCategories == null) {
            testResultCategories = "";
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

    /**
     * Run on Jenkins shutdown.
     */
    @Terminator
    public void stop() {
        ScanOnDemandQueue.shutdown();
        knowledgeBase.stop();
    }

    /**
     * Returns the base relative URI for static resources packaged in webapp.
     *
     * @return the base URI.
     */
    public static String getStaticResourcesBase() {
        return "/plugin/build-failure-analyzer";
    }

    /**
     * Getter sodVariable.
     *
     * @return the message.
     */
    public ScanOnDemandVariables getSodVariables() {
        return sodVariables;
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
        return Jenkins.getInstance().getRootUrl() + getImageUrl(size, name);
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
    @Nonnull
    public static PluginImpl getInstance() {
        return ExtensionList.lookup(PluginImpl.class).get(0);
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
     * If this feature is enabled or not. When on all aborted builds will be ignored.
     *
     * @return true if on.
     */
    public boolean isDoNotAnalyzeAbortedJob() {
        return doNotAnalyzeAbortedJob;
    }

    /**
     * If graphs are enabled or not. Links to graphs and graphs will not be displayed when disabled.
     * It can be enabled only if the knowledgeBase has support for it.
     * @return True if enabled.
     */
    public boolean isGraphsEnabled() {
        if (graphsEnabled == null || knowledgeBase == null) {
            return false;
        } else {
            return knowledgeBase.isStatisticsEnabled() && graphsEnabled;
        }
    }

    /**
     * Sets if graphs are enabled.
     * @param graphsEnabled the graph flag
     */
    @DataBoundSetter
    public void setGraphsEnabled(Boolean graphsEnabled) {
        this.graphsEnabled = graphsEnabled;
    }

    /**
     * Sets the no causes message.
     * @param noCausesMessage the no causes message
     */
    @DataBoundSetter
    public void setNoCausesMessage(String noCausesMessage) {
        this.noCausesMessage = noCausesMessage;
    }

    /**
     * Sets the knowledge base.
     * @param knowledgeBase the knowledge base
     */
    @DataBoundSetter
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Sets the scan on demand variables.
     * @param sodVariables the variables
     */
    @DataBoundSetter
    public void setSodVariables(ScanOnDemandVariables sodVariables) {
        this.sodVariables = sodVariables;
    }

    /**
     * If failed test cases should be represented as failure causes.
     *
     * @return True if enabled.
     */
    public boolean isTestResultParsingEnabled() {
        if (testResultParsingEnabled == null) {
            return false;
        } else {
            return testResultParsingEnabled;
        }
    }

    /**
     * Get categories to be assigned to failure causes representing failed test cases.
     *
     * @return the categories.
     */
    public String getTestResultCategories() {
        return testResultCategories;
    }

    /**
     * Sets if this feature is enabled or not. When on all aborted builds will be ignored.
     *
     * @param doNotAnalyzeAbortedJob on or off.
     */
    public void setDoNotAnalyzeAbortedJob(boolean doNotAnalyzeAbortedJob) {
        this.doNotAnalyzeAbortedJob = doNotAnalyzeAbortedJob;
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
     * Sets if failed test cases should be represented as failure causes or not.
     *
     * @param testResultParsingEnabled on or off. null == off.
     */
    public void setTestResultParsingEnabled(Boolean testResultParsingEnabled) {
        this.testResultParsingEnabled = testResultParsingEnabled;
    }

    /**
     * Set categories to be assigned to failure causes representing failed test cases.
     *
     * @param testResultCategories Space-separated string with categories
     */
    public void setTestResultCategories(String testResultCategories) {
        this.testResultCategories = testResultCategories;
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
     * Set the maximum log size that should be scanned.
     *
     * @param maxLogSize value
     */
    public void setMaxLogSize(int maxLogSize) {
        this.maxLogSize = maxLogSize;
    }

    /**
     * Returns the maximum log size that should be scanned.
     *
     * @return value
     */
    public int getMaxLogSize() {
        if (maxLogSize < 0) {
            return DEFAULT_MAX_LOG_SIZE;
        }

        return maxLogSize;
    }


    /**
     * Checks if the build with certain result should be analyzed or not.
     *
     * @param result the result
     * @return true if it should be analyzed.
     */
    public static boolean needToAnalyze(Result result)  {
        if (getInstance().isDoNotAnalyzeAbortedJob()) {
            return result != Result.SUCCESS && result != Result.ABORTED;
        }   else {
            return result != Result.SUCCESS;
        }
    }

    /**
     * Checks if the specified build should be scanned or not.
     *
     * @param build the build
     * @return true if it should be scanned.
     * @see #shouldScan(Job)
     */
    public static boolean shouldScan(Run build) {
        return shouldScan(build.getParent());
    }

    /**
     * Checks that log size is in limits.
     *
     * @param build the build
     * @return true if size is in limit.
     */
    public static boolean isSizeInLimit(Run build) {
        return getInstance().getMaxLogSize() == 0
                || getInstance().getMaxLogSize() > (build.getLogFile().length() / BYTES_IN_MEGABYTE);
    }

    /**
     * Checks if the specified project should be scanned or not. Determined by {@link #isGlobalEnabled()} and if the
     * project has {@link com.sonyericsson.jenkins.plugins.bfa.model.ScannerJobProperty#isDoNotScan()}.
     *
     * @param project the project
     * @return true if it should be scanned.
     */
    public static boolean shouldScan(Job project) {
        if (getInstance().isGlobalEnabled()) {
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
    public boolean configure(StaplerRequest req, JSONObject o) {
        KnowledgeBase existingKb = this.knowledgeBase;
        req.bindJSON(this, o);

        if (existingKb != null && !existingKb.equals(knowledgeBase)) {
            if (o.getBoolean("convertOldKb")) {
                try {
                    knowledgeBase.start();
                    knowledgeBase.convertFrom(existingKb);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not convert knowledge base ", e);
                }
                knowledgeBase.stop();
            }
        }

        save();
        return true;
    }
}
