package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Development Plan Review and Code Generation Routes.
 */
@CamelSpringBootTest
@SpringBootTest
@MockEndpoints("seda:*")
@TestPropertySource(properties = {
    "app.jira.base-url=http://localhost:9090",
    "app.jira.username=test-user",
    "app.jira.api-token=test-token",
    "app.jira.project-key=TEST",
    "app.copilot.api-url=http://localhost:9091",
    "app.copilot.api-key=test-key",
    "app.routes.refinement-schedule=3600000",
    "app.routes.dev-plan-schedule=3600000",
    "app.routes.dev-plan-review-schedule=3600000",
    "app.routes.code-gen-schedule=3600000",
    "camel.http.jira-search-uri=http://localhost:9090/rest/api/3/search",
    "camel.http.jira-issue-uri=http://localhost:9090/rest/api/3/issue",
    "camel.http.jira-create-issue-uri=http://localhost:9090/rest/api/3/issue",
    "camel.http.copilot-completions-uri=http://localhost:9091/chat/completions"
})
class ReviewAndCodeGenRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private JiraProcessor jiraProcessor;

    @Autowired
    private CopilotProcessor copilotProcessor;

    @Test
    void testReviewRoutesAreRegistered() {
        assertNotNull(camelContext.getRoute("route-devplan-review-poll"));
        assertNotNull(camelContext.getRoute("route-devplan-review-process"));
    }

    @Test
    void testCodeGenRoutesAreRegistered() {
        assertNotNull(camelContext.getRoute("route-codegen-poll"));
        assertNotNull(camelContext.getRoute("route-codegen-process"));
    }

    @Test
    void testBuildJqlQueryForAI07() {
        String jql = jiraProcessor.buildJqlQuery(AiState.AI07_WAITING_DEV_PLAN_REVIEW);
        assertTrue(jql.contains("AI07"));
        assertTrue(jql.contains("AI-Agent"));
    }

    @Test
    void testBuildJqlQueryForAI10() {
        String jql = jiraProcessor.buildJqlQuery(AiState.AI10_WAITING_CODE_GEN);
        assertTrue(jql.contains("AI10"));
        assertTrue(jql.contains("AI-Agent"));
    }

    @Test
    void testBuildDevPlanReviewPrompt() {
        String prompt = copilotProcessor.buildDevPlanReviewPrompt(
            "Payment Task",
            "Development plan details..."
        );
        assertNotNull(prompt);
        assertTrue(prompt.contains("Payment Task"));
        assertTrue(prompt.contains("Development plan details"));
    }

    @Test
    void testBuildCodeGenPrompt() {
        String prompt = copilotProcessor.buildCodeGenPrompt(
            "Payment Task",
            "Development plan details..."
        );
        assertNotNull(prompt);
        assertTrue(prompt.contains("Payment Task"));
        assertTrue(prompt.contains("Development plan details"));
    }

    @Test
    void testBasicAuthHeaderFormat() {
        String authHeader = jiraProcessor.buildBasicAuthHeader();
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }
}
