package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.dto.JiraTaskDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Development Plan Generation Route.
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
class DevelopmentPlanGenerationRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private JiraProcessor jiraProcessor;

    @Autowired
    private CopilotProcessor copilotProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertNotNull(camelContext);
    }

    @Test
    void testDevPlanRoutesAreRegistered() {
        assertNotNull(camelContext.getRoute("route-devplan-poll"));
        assertNotNull(camelContext.getRoute("route-devplan-process"));
    }

    @Test
    void testBuildJqlQueryForAI04() {
        String jql = jiraProcessor.buildJqlQuery("AI04");
        assertNotNull(jql);
        assertTrue(jql.contains("AI04"));
        assertTrue(jql.contains("AI-Agent"));
    }

    @Test
    void testBuildDevPlanPrompt() {
        String prompt = copilotProcessor.buildDevPlanPrompt(
            "Implement payment service",
            "Build a payment processing microservice"
        );
        assertNotNull(prompt);
        assertTrue(prompt.contains("Implement payment service"));
        assertTrue(prompt.contains("Build a payment processing microservice"));
    }

    @Test
    void testBuildAiStateUpdatePayload() throws Exception {
        Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload("AI05 - Dev plan generation in progress");
        assertNotNull(payload);
        assertTrue(payload.containsKey("fields"));
        Map<?, ?> fields = (Map<?, ?>) payload.get("fields");
        assertEquals("AI05 - Dev plan generation in progress", fields.get(appConfig.getJira().getAiExchangeTrackingFieldId()));
    }

    @Test
    void testBuildCreateTaskPayload() throws Exception {
        JiraTaskDto task = new JiraTaskDto(
            "Development Plan: Payment Service",
            "Detailed development plan...",
            "TEST-1",
            "TEST"
        );
        task.setAiExchangeTracking("AI07 - Waiting for dev plan review");

        Map<String, Object> payload = jiraProcessor.buildCreateTaskPayload(task);
        assertNotNull(payload);
        assertTrue(payload.containsKey("fields"));
        Map<?, ?> fields = (Map<?, ?>) payload.get("fields");
        assertEquals("Development Plan: Payment Service", fields.get("summary"));
        assertEquals("Task", ((Map<?, ?>) fields.get("issuetype")).get("name"));

        String json = objectMapper.writeValueAsString(payload);
        assertNotNull(json);
        assertTrue(json.contains("Development Plan"));
    }
}
