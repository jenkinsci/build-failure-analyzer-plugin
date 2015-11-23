package com.sonyericsson.jenkins.plugins.bfa;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Hudson;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.SecurityRealm;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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

    /**
     * Configures Jenkins to use security and defines several users with different rights for the
     * management or view of failure causes.
     */
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

    /**
     * Checks that a non authorised user cannot access the failure management page at all.
     *
     * @throws java.lang.Exception If Jenkins cannot be accessed
     */
    @Test
    public void notAllowedToUpdateCausesWhenNotGrantedAnything() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("none");
        // Gets to the Failure Cause page
        try {
            webClient.goTo("failure-cause-management");
            fail("Access to the page should have failed");
        } catch (FailingHttpStatusCodeException ex) {
            assertEquals(HttpServletResponse.SC_FORBIDDEN, ex.getStatusCode());
        }
    }

    /**
     * Checks that a user granted with "viewCauses" only can access the failure management page
     * <i>but not</i> create a new failure.
     *
     * @throws java.lang.Exception If Jenkins cannot be accessed
     */
    @Test
    public void allowedToViewCausesWhenGrantedOnlyView() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("view");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.selectSingleNode("//h1[.='List of Failure Causes']"));
        // Checks the "Create New" button is NOT available
        assertNull(page.selectSingleNode("//a[.='Create new']"));
    }

    /**
     * Checks that a user granted with "updateCauses" only can access the failure management page
     * <i>and</i> create a new failure.
     *
     * @throws java.lang.Exception If Jenkins cannot be accessed
     */
    @Test
    public void allowedToUpdateCausesWhenGrantedOnlyUpdate() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("update");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.selectSingleNode("//h1[.='Update Failure Causes']"));
        // Checks the "Create New" button is available
        assertNotNull(page.selectSingleNode("//a[.='Create new']"));
    }

    /**
     * Checks that a user granted with "updateCauses" and "viewCauses" only can access the failure management page
     * <i>and</i> create a new failure.
     *
     * @throws java.lang.Exception If Jenkins cannot be accessed
     */
    @Test
    public void allowedToUpdateCausesWhenGrantedBothUpdateAndView() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("all");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.selectSingleNode("//h1[.='Update Failure Causes']"));
        // Checks the "Create New" button is available
        assertNotNull(page.selectSingleNode("//a[.='Create new']"));
    }
}
