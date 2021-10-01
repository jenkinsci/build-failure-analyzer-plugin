package com.sonyericsson.jenkins.plugins.bfa.jcasc;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseModification;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Checks configuration as code integration for mongo DB.
 */
public class ConfigurationAsCodeMongoTest {

    /**
     * Jenkins rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    /**
     * Support config as code import.
     */
    @Test
    @ConfiguredWithCode("jcasc-mongo.yml")
    public void shouldSupportConfigurationAsCode() {
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
    }

    /**
     * Support config as code export.
     *
     * @throws Exception if so.
     */
    @Test
    @ConfiguredWithCode("jcasc-mongo.yml")
    public void shouldSupportConfigurationExport() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute)
                .replaceAll(".+password: .+", ""); // ignore dynamic password secret

        String expected = toStringFromYamlFile(this, "jcasc-mongo-expected.yml");

        assertThat(exported, is(expected));
    }

    /**
     * Support config as code import with a minimal definition.
     */
    @Test
    @ConfiguredWithCode("jcasc-mongo-less.yml")
    public void shouldSupportConfigurationAsCodeWithLessCauseParameters() {
        PluginImpl plugin = PluginImpl.getInstance();

        MongoDBKnowledgeBase knowledgeBase = (MongoDBKnowledgeBase)plugin.getKnowledgeBase();

        List<FailureCause> initialCauses = new ArrayList<>(knowledgeBase.getCauses());
        assertThat(initialCauses.size(), is(1));

        FailureCause cause = initialCauses.get(0);
        assertThat(cause.getId(), is(ConfigurationAsCodeLocalTest.EXPECTED_ID));
        assertThat(cause.getDescription(), is(ConfigurationAsCodeLocalTest.EXPECTED_DESCRIPTION));
        assertThat(cause.getComment(), is(nullValue()));
        assertThat(cause.getCategoriesAsString(), is(nullValue()));
        assertThat(cause.getName(), is(ConfigurationAsCodeLocalTest.EXPECTED_NAME));

        assertThat(cause.getIndications().size(), is(1));
        assertThat(cause.getIndications().get(0), instanceOf(BuildLogIndication.class));
        BuildLogIndication buildLog = (BuildLogIndication)cause.getIndications().get(0);
        assertThat(buildLog.getUserProvidedExpression(), is(ConfigurationAsCodeLocalTest.EXPECTED_BUILD_LOG_EXPRESSION));

        assertThat(cause.getModifications().size(), is(1));
        FailureCauseModification modification = cause.getModifications().get(0);
        assertThat(modification.getTime(), is(notNullValue()));
        assertThat(modification.getTime().getTime(), is(ConfigurationAsCodeLocalTest.EXPECTED_MODIFICATION_TIME));
        assertThat(modification.getUser(), is(ConfigurationAsCodeLocalTest.EXPECTED_USER));
    }

    /**
     * Not enabling causes export prevents causes from being exported.
     */
    @Test
    @ConfiguredWithCode("jcasc-mongo-no-export.yml")
    public void shouldNotExportCausesIfFeatureIsNotEnabled() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute)
                .replaceAll(".+password: .+", ""); // ignore dynamic password secret

        String expected = toStringFromYamlFile(this, "jcasc-mongo-no-export-expected.yml");

        assertThat(exported, is(expected));
    }
}
