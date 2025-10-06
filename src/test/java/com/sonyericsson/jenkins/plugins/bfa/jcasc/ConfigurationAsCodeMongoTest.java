package com.sonyericsson.jenkins.plugins.bfa.jcasc;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
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

/**
 * Checks configuration as code integration for mongo DB.
 */
@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeMongoTest {

    /**
     * Support config as code import.
     *
     * @param j
     *
     */
    @Test
    @ConfiguredWithCode("jcasc-mongo.yml")
    void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule j) {
        PluginImpl plugin = PluginImpl.getInstance();

        assertThat(plugin.isDoNotAnalyzeAbortedJob(), is(true));
        assertThat(plugin.isGerritTriggerEnabled(), is(true));
        assertThat(plugin.isGlobalEnabled(), is(true));
        assertThat(plugin.isGraphsEnabled(), is(false));

        MongoDBKnowledgeBase knowledgeBase = (MongoDBKnowledgeBase)plugin.getKnowledgeBase();
        assertThat(knowledgeBase.getHost(), is("localhost"));
        assertThat(knowledgeBase.getDbName(), is("bfa"));
        assertThat(knowledgeBase.isEnableStatistics(), is(true));
        assertThat(knowledgeBase.getUserName(), is("bfa"));
        assertThat(knowledgeBase.getPassword().getPlainText(), is("changeme"));
        assertThat(knowledgeBase.isSuccessfulLogging(), is(false));

        assertThat(plugin.getNoCausesMessage(), is(ConfigurationAsCodeLocalTest.NO_CAUSES_MESSAGE));

        assertThat(plugin.getNrOfScanThreads(), is(ConfigurationAsCodeLocalTest.EXPECTED_SCAN_THREADS));

        ScanOnDemandVariables sodVariables = plugin.getSodVariables();

        assertThat(sodVariables.getMaximumSodWorkerThreads(),
                is(ConfigurationAsCodeLocalTest.EXPECTED_MAXIMUM_SOD_WORKER_THREADS));
        assertThat(sodVariables.getMinimumSodWorkerThreads(),
                is(ConfigurationAsCodeLocalTest.EXPECTED_MINIMUM_SOD_WORKER_THREADS));
        assertThat(sodVariables.getSodCorePoolNumberOfThreads(),
                is(ConfigurationAsCodeLocalTest.EXPECTED_SOD_CORE_POOL_NUMBER_OF_THREADS));
        assertThat(sodVariables.getSodThreadKeepAliveTime(),
                is(ConfigurationAsCodeLocalTest.EXPECTED_SOD_THREAD_KEEP_ALIVE_TIME));
        assertThat(sodVariables.getSodWaitForJobShutdownTimeout(),
                is(ConfigurationAsCodeLocalTest.EXPECTED_SOD_JOB_SHUTDOWN_TIMEOUT));

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
    @ConfiguredWithCode("jcasc-mongo.yml")
    void shouldSupportConfigurationExport(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute)
                .replaceAll(".+password: .+", ""); // ignore dynamic password secret

        String expected = toStringFromYamlFile(this, "jcasc-mongo-expected.yml");

        assertThat(exported, is(expected));
    }
}
