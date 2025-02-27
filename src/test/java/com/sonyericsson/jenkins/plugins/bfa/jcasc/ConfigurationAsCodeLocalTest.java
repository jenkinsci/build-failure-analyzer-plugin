package com.sonyericsson.jenkins.plugins.bfa.jcasc;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.jupiter.api.Test;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

/**
 * Checks configuration as code integration for local DB.
 */
@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeLocalTest {

    static final String NO_CAUSES_MESSAGE = "No problems were identified. Please contribute  causes to help others";
    static final int EXPECTED_SCAN_THREADS = 6;
    static final int EXPECTED_MAXIMUM_SOD_WORKER_THREADS = 7;
    static final int EXPECTED_MINIMUM_SOD_WORKER_THREADS = 2;
    static final int EXPECTED_SOD_CORE_POOL_NUMBER_OF_THREADS = 6;
    static final int EXPECTED_SOD_THREAD_KEEP_ALIVE_TIME = 17;
    static final int EXPECTED_SOD_JOB_SHUTDOWN_TIMEOUT = 32;

    /**
     * Support config as code import.
     *
     * @param j
     *
     */
    @Test
    @ConfiguredWithCode("jcasc-local.yml")
    void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule j) {
        PluginImpl plugin = PluginImpl.getInstance();

        assertThat(plugin.isDoNotAnalyzeAbortedJob(), is(true));
        assertThat(plugin.isGerritTriggerEnabled(), is(true));
        assertThat(plugin.isGlobalEnabled(), is(true));
        assertThat(plugin.isGraphsEnabled(), is(false));
        assertThat(plugin.getKnowledgeBase(), instanceOf(LocalFileKnowledgeBase.class));
        assertThat(plugin.getNoCausesMessage(), is(NO_CAUSES_MESSAGE));

        assertThat(plugin.getNrOfScanThreads(), is(EXPECTED_SCAN_THREADS));

        ScanOnDemandVariables sodVariables = plugin.getSodVariables();

        assertThat(sodVariables.getMaximumSodWorkerThreads(), is(EXPECTED_MAXIMUM_SOD_WORKER_THREADS));
        assertThat(sodVariables.getMinimumSodWorkerThreads(), is(EXPECTED_MINIMUM_SOD_WORKER_THREADS));
        assertThat(sodVariables.getSodCorePoolNumberOfThreads(), is(EXPECTED_SOD_CORE_POOL_NUMBER_OF_THREADS));
        assertThat(sodVariables.getSodThreadKeepAliveTime(), is(EXPECTED_SOD_THREAD_KEEP_ALIVE_TIME));
        assertThat(sodVariables.getSodWaitForJobShutdownTimeout(), is(EXPECTED_SOD_JOB_SHUTDOWN_TIMEOUT));

        assertThat(plugin.getTestResultCategories(), is("hgjghhlllllaa"));
        assertThat(plugin.isTestResultParsingEnabled(), is(true));

        assertThat(plugin.isMetricSquashingEnabled(), is(false));
    }

    /**
     * Support config as code export.
     *
     * @param j
     *
     * @throws Exception if so.
     */
    @Test
    @ConfiguredWithCode("jcasc-local.yml")
    void shouldSupportConfigurationExport(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "jcasc-local-expected.yml");

        assertThat(exported, is(expected));
    }
}
