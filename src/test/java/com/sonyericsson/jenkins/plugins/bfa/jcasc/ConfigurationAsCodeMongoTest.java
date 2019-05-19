package com.sonyericsson.jenkins.plugins.bfa.jcasc;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.LocalFileKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables;
import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class ConfigurationAsCodeMongoTest {

    private static final String NO_CAUSES_MESSAGE = "No problems were identified. If you know why this problem " +
            "occurred, please add a suitable Cause for it.";

    @ClassRule
    @ConfiguredWithCode("jcasc-mongo.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void should_support_configuration_as_code() {
        PluginImpl plugin = PluginImpl.getInstance();

        assertThat(plugin.isDoNotAnalyzeAbortedJob(), is(true));
        assertThat(plugin.isGerritTriggerEnabled(), is(true));
        assertThat(plugin.isGlobalEnabled(), is(true));
        assertThat(plugin.isGraphsEnabled(), is(false));

        MongoDBKnowledgeBase knowledgeBase = (MongoDBKnowledgeBase) plugin.getKnowledgeBase();
        assertThat(knowledgeBase.getHost(), is("localhost"));
        assertThat(knowledgeBase.getDbName(), is("bfa"));
        assertThat(knowledgeBase.isStatisticsEnabled(), is(true));
        assertThat(knowledgeBase.getUserName(), is("bfa"));
        assertThat(knowledgeBase.getPassword().getPlainText(), is("changeme"));
        assertThat(knowledgeBase.isSuccessfulLoggingEnabled(), is(false));

        assertThat(plugin.getNoCausesMessage(), is(NO_CAUSES_MESSAGE));

        assertThat(plugin.getNrOfScanThreads(), is(6));
        ScanOnDemandVariables sodVariables = plugin.getSodVariables();
        assertThat(sodVariables.getMaximumSodWorkerThreads(), is(4));
        assertThat(sodVariables.getMinimumSodWorkerThreads(), is(2));
        assertThat(sodVariables.getSodCorePoolNumberOfThreads(), is(6));
        assertThat(sodVariables.getSodThreadKeepAliveTime(), is(17));
        assertThat(sodVariables.getSodWaitForJobShutdownTimeout(), is(32));

        assertThat(plugin.getTestResultCategories(), is("hgjghhlllllaa"));
        assertThat(plugin.isTestResultParsingEnabled(), is(true));
    }

    @Test
    public void should_support_configuration_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("buildFailureAnalyzer");

        String exported = toYamlString(yourAttribute)
                .replaceAll(".+password: .+", ""); // ignore dynamic password secret

        String expected = toStringFromYamlFile(this, "jcasc-mongo-expected.yml");

        assertThat(exported, is(expected));
    }
}