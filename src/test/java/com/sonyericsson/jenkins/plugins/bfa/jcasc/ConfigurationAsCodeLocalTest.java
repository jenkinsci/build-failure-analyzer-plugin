package com.sonyericsson.jenkins.plugins.bfa.jcasc;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseModification;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.BuildLogIndication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.Indication;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.MultilineBuildLogIndication;
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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Checks configuration as code integration for local DB.
 */
public class ConfigurationAsCodeLocalTest {

    static final String NO_CAUSES_MESSAGE = "No problems were identified. Please contribute  causes to help others";
    static final int EXPECTED_SCAN_THREADS = 6;
    static final int EXPECTED_MAXIMUM_SOD_WORKER_THREADS = 4;
    static final int EXPECTED_MINIMUM_SOD_WORKER_THREADS = 2;
    static final int EXPECTED_SOD_CORE_POOL_NUMBER_OF_THREADS = 6;
    static final int EXPECTED_SOD_THREAD_KEEP_ALIVE_TIME = 17;
    static final int EXPECTED_SOD_JOB_SHUTDOWN_TIMEOUT = 32;

    static final String EXPECTED_ID = "035e67d4-6692-4b1f-adf9-95c43948b349";
    static final String EXPECTED_DESCRIPTION = "A problem was found";
    static final String EXPECTED_COMMENT = "To show what a cause looks like";
    static final String EXPECTED_CATEGORIES = "example second";
    static final String EXPECTED_NAME = "Found problems";
    static final String EXPECTED_BUILD_LOG_EXPRESSION = ".*problem.*";
    static final String EXPECTED_MULTILINE_BUILD_LOG_EXPRESSION = "many.*problems";
    static final String EXPECTED_USER = "Somebody";
    static final long EXPECTED_MODIFICATION_TIME = 1611951507000L;

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
    @ConfiguredWithCode("jcasc-local.yml")
    public void shouldSupportConfigurationAsCode() {
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

        List<FailureCause> initialCauses = new ArrayList<FailureCause>(plugin.getCauses());
        assertThat(initialCauses.size(), is(1));
        FailureCause cause = initialCauses.get(0);
        assertThat(cause.getId(), is(EXPECTED_ID));
        assertThat(cause.getDescription(), is(EXPECTED_DESCRIPTION));
        assertThat(cause.getComment(), is(EXPECTED_COMMENT));
        assertThat(cause.getCategoriesAsString(), is(EXPECTED_CATEGORIES));
        assertThat(cause.getName(), is(EXPECTED_NAME));

        List<Indication> indications = cause.getIndications();
        assertThat(indications.size(), is(2));
        assertThat(indications.get(0), instanceOf(BuildLogIndication.class));
        assertThat(indications.get(1), instanceOf(MultilineBuildLogIndication.class));
        BuildLogIndication buildLog = (BuildLogIndication)indications.get(0);
        assertThat(buildLog.getUserProvidedExpression(), is(EXPECTED_BUILD_LOG_EXPRESSION));
        MultilineBuildLogIndication multilineBuildLog = (MultilineBuildLogIndication)indications.get(1);
        assertThat(multilineBuildLog.getUserProvidedExpression(), is(EXPECTED_MULTILINE_BUILD_LOG_EXPRESSION));
    }

    /**
     * Support config as code import with a minimal definition.
     */
    @Test
    @ConfiguredWithCode("jcasc-local-less.yml")
    public void shouldSupportConfigurationAsCodeWithLessCauseParameters() {
        PluginImpl plugin = PluginImpl.getInstance();

        List<FailureCause> initialCauses = new ArrayList<>(plugin.getCauses());
        assertThat(initialCauses.size(), is(1));

        FailureCause cause = initialCauses.get(0);
        assertThat(cause.getId(), is(EXPECTED_ID));
        assertThat(cause.getDescription(), is(EXPECTED_DESCRIPTION));
        assertThat(cause.getComment(), is(nullValue()));
        assertThat(cause.getCategoriesAsString(), is(nullValue()));
        assertThat(cause.getName(), is(EXPECTED_NAME));

        assertThat(cause.getIndications().size(), is(1));
        assertThat(cause.getIndications().get(0), instanceOf(BuildLogIndication.class));
        BuildLogIndication buildLog = (BuildLogIndication)cause.getIndications().get(0);
        assertThat(buildLog.getUserProvidedExpression(), is(EXPECTED_BUILD_LOG_EXPRESSION));

        assertThat(cause.getModifications().size(), is(1));
        FailureCauseModification modification = cause.getModifications().get(0);
        assertThat(modification.getTime(), is(notNullValue()));
        assertThat(modification.getTime().getTime(), is(EXPECTED_MODIFICATION_TIME));
        assertThat(modification.getUser(), is(EXPECTED_USER));
    }

    /**
     * Not enabling causes export prevents causes from being exported.
     */
    @Test
    @ConfiguredWithCode("jcasc-local-no-export.yml")
    public void shouldNotExportCausesIfFeatureIsNotEnabled() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "jcasc-local-no-export-expected.yml");

        assertThat(exported, is(expected));
    }

    /**
     * Support config as code export.
     *
     * @throws Exception if so.
     */
    @Test
    @ConfiguredWithCode("jcasc-local.yml")
    public void shouldSupportConfigurationExport() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "jcasc-local-expected.yml");

        assertThat(exported, is(expected));
    }
}
