package com.sonyericsson.jenkins.plugins.bfa;

import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlPage;
import hudson.model.Hudson;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.SecurityRealm;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import jakarta.servlet.http.HttpServletResponse;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the permissions for the Cause Management.
 *
 * @author Damien Coraboeuf
 */
@WithJenkins
class CauseManagementPermissionTest {

    private static final int EXPECTED_HTTP_NOT_FOUND_RESPONSE_CODE = 404;
    private static final int EXPECTED_HTTP_FORBIDDEN_RESPONSE_CODE = 403;
    private static final int EXPECTED_HTTP_SUCCESS_RESPONSE_CODE = 200;

    private JenkinsRule j;

    /**
     * Configures Jenkins to use security and defines several users with different rights for the
     * management or view of failure causes.
     *
     * @param rule
     */
    @BeforeEach
    void jenkinsConfiguration(JenkinsRule rule) {
        j = rule;
        SecurityRealm securityRealm = j.createDummySecurityRealm();
        j.getInstance().setSecurityRealm(securityRealm);

        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Hudson.READ, new PermissionEntry(AuthorizationType.EITHER, "anonymous"));
        authorizationStrategy.add(PluginImpl.VIEW_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "view"));
        authorizationStrategy.add(
                PluginImpl.UPDATE_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "update"));
        authorizationStrategy.add(PluginImpl.REMOVE_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "remove"));
        authorizationStrategy.add(PluginImpl.VIEW_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "all"));
        authorizationStrategy.add(PluginImpl.UPDATE_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "all"));
        authorizationStrategy.add(PluginImpl.REMOVE_PERMISSION, new PermissionEntry(AuthorizationType.EITHER, "all"));
        authorizationStrategy.add(Jenkins.ADMINISTER, new PermissionEntry(AuthorizationType.EITHER, "admin"));
        j.getInstance().setAuthorizationStrategy(authorizationStrategy);
    }

    /**
     * Checks that a non-authorised user cannot access the failure management page at all.
     *
     * @throws Exception If Jenkins cannot be accessed
     */
    @Test
    void notAllowedToUpdateCausesWhenNotGrantedAnything() throws Exception {
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
     * @throws Exception If Jenkins cannot be accessed
     */
    @Test
    void allowedToViewCausesWhenGrantedOnlyView() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("view");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.getFirstByXPath("//h1[contains(text(), 'List of Failure Causes')]"));
        // Checks the "Create New" button is NOT available
        assertNull(page.getFirstByXPath("//a[.='Create new']"));
    }

    /**
     * Checks that a user granted with "updateCauses" only can access the failure management page
     * <i>and</i> create a new failure.
     *
     * @throws Exception If Jenkins cannot be accessed
     */
    @Test
    void allowedToUpdateCausesWhenGrantedOnlyUpdate() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("update");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.getFirstByXPath("//h1[contains(text(),'Update Failure Causes')]"));
        // Checks the "Create New" button is available
        assertNotNull(page.getFirstByXPath("//a[.='Create new']"));
    }

    /**
     * Checks that a user granted with "updateCauses" and "viewCauses" only can access the failure management page
     * <i>and</i> create a new failure.
     *
     * @throws Exception If Jenkins cannot be accessed
     */
    @Test
    void allowedToUpdateCausesWhenGrantedBothUpdateAndView() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        // Logs in
        webClient.goTo("");
        webClient.login("all");
        // Gets to the Failure Cause page
        HtmlPage page = webClient.goTo("failure-cause-management");
        // Checks we are actually on the page
        assertNotNull(page.getFirstByXPath("//h1[contains(text(), 'Update Failure Causes')]"));
        // Checks the "Create New" button is available
        assertNotNull(page.getFirstByXPath("//a[.='Create new']"));
    }

    /**
     * Tests that removeConfirm only can be used with POST, and responds with 404 otherwise.
     *
     * @throws Exception If Jenkins cannot be accessed
     */
    @Issue("SECURITY-3239")
    @Test
    void testDoRemoveConfirmRequiresPost() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        webClient.login("all");
        assertEquals(
                EXPECTED_HTTP_NOT_FOUND_RESPONSE_CODE,
                webClient.goTo("failure-cause-management/removeConfirm").getWebResponse().getStatusCode());
        WebRequest webRequest = new WebRequest(
                new URL(j.jenkins.getRootUrl() + "/failure-cause-management/removeConfirm"),
                HttpMethod.POST);
        webRequest = webClient.addCrumb(webRequest);
        assertEquals(
                EXPECTED_HTTP_SUCCESS_RESPONSE_CODE,
                webClient.getPage(webRequest).getWebResponse().getStatusCode());
    }

    /**
     * Test that testing mongo connection can only be accessed through a POST request from an admin.
     *
     * @throws Exception if Jenkins cannot be accessed
     */
    @Issue("SECURITY-3226")
    @Test
    void testTestMongoDBConnection() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        String testUrl = "descriptorByName/com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase/"
                + "testConnection?port=9876&host=localhost&&dbName=Whatever\n";
        assertEquals(
                EXPECTED_HTTP_NOT_FOUND_RESPONSE_CODE,
                webClient.goTo(testUrl).getWebResponse().getStatusCode());
        webClient.login("all");
        WebRequest webRequest = new WebRequest(
                new URL(j.jenkins.getRootUrl() + testUrl),
                HttpMethod.POST);
        webRequest = webClient.addCrumb(webRequest);
        assertEquals(
                EXPECTED_HTTP_FORBIDDEN_RESPONSE_CODE,
                webClient.getPage(webRequest).getWebResponse().getStatusCode());
        webClient.login("admin");
        assertEquals(
                EXPECTED_HTTP_SUCCESS_RESPONSE_CODE,
                webClient.getPage(webRequest).getWebResponse().getStatusCode());
    }
}
