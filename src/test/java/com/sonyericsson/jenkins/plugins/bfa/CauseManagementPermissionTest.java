package com.sonyericsson.jenkins.plugins.bfa;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Hudson;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.SecurityRealm;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests the permissions for the Cause Management.
 *
 * @author Damien Coraboeuf
 */
public class CauseManagementPermissionTest {

    /**
     * The Jenkins Rule.
     */
    @Rule
    //CS IGNORE VisibilityModifier FOR NEXT 1 LINES. REASON: Jenkins Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void jenkinsConfiguration() {
        SecurityRealm securityRealm = j.createDummySecurityRealm();
        j.getInstance().setSecurityRealm(securityRealm);

        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Hudson.READ, "anonymous");
        authorizationStrategy.add(PluginImpl.VIEW_PERMISSION, "view");
        authorizationStrategy.add(PluginImpl.UPDATE_PERMISSION, "update");
        authorizationStrategy.add(PluginImpl.VIEW_PERMISSION, "all");
        authorizationStrategy.add(PluginImpl.UPDATE_PERMISSION, "all");
        j.getInstance().setAuthorizationStrategy(authorizationStrategy);
    }

    @Test
    public void notAllowedToUpdateCausesWhenNotGrantedAnything() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("none");
        // Gets to the Failure Cause page
        webClient.goTo("failure-cause-management");
        // FIXME Expects a failure
    }

    @Test
    public void allowedToUpdateCausesWhenGrantedOnlyUpdate() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("update");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // FIXME Checks the "Create New" button is available
    }
}
