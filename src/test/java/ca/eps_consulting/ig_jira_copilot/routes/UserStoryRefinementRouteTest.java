package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the User Story Refinement Route.
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
class UserStoryRefinementRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private JiraProcessor jiraProcessor;

    @Autowired
    private CopilotProcessor copilotProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    @EndpointInject("mock:seda:refinement-process")
    private MockEndpoint mockRefinementSeda;

    @BeforeEach
    void setUp() throws Exception {
        mockRefinementSeda.reset();
    }

    @Test
    void contextLoads() {
        assertNotNull(camelContext);
        assertNotNull(appConfig);
    }

    @Test
    void testRefinementRouteIsRegistered() {
        assertNotNull(camelContext.getRoute("route-refinement-poll"));
        assertNotNull(camelContext.getRoute("route-refinement-process"));
    }

    @Test
    void testAppConfigIsPopulated() {
        assertEquals("http://localhost:9090", appConfig.getJira().getBaseUrl());
        assertEquals("test-user", appConfig.getJira().getUsername());
        assertEquals("TEST", appConfig.getJira().getProjectKey());
        assertEquals("http://localhost:9091", appConfig.getCopilot().getApiUrl());
    }

    @Test
    void testBuildJqlQueryForAI01() {
        String jql = jiraProcessor.buildJqlQuery(AiState.AI01_WAITING_REFINEMENT);
        assertNotNull(jql);
        assert jql.contains("AI01");
        assert jql.contains("AI-Agent");
    }

    @Test
    void testBuildRefinementPrompt() {
        String prompt = copilotProcessor.buildRefinementPrompt(
                "As a user, I want to login",
                "Implement OAuth2 login flow"
        );
        assertNotNull(prompt);
        assert prompt.contains("As a user, I want to login");
        assert prompt.contains("Implement OAuth2 login flow");
    }

    @Test
    void testJiraIssueDto() {
        JiraIssueDto issue = new JiraIssueDto();
        issue.setKey("TEST-123");
        JiraIssueDto.Fields fields = new JiraIssueDto.Fields();
        fields.setSummary("Test Summary");
        // fields.setDescription("Test Description");
        fields.setLabels(List.of("AI-Agent"));
        fields.setAiExchangeTracking("AI01 - Waiting for refinement");
        issue.setFields(fields);

        assertEquals("TEST-123", issue.getKey());
        assertEquals("Test Summary", issue.getFields().getSummary());
        assertEquals("AI01 - Waiting for refinement", issue.getFields().getAiExchangeTracking());
    }

    @Test
    void testJiraSearchResultDeserialization() throws Exception {
        String json = """
                {
                  "total": 1,
                  "startAt": 0,
                  "maxResults": 50,
                  "issues": [
                    {
                      "id": "10001",
                      "key": "TEST-1",
                      "fields": {
                        "summary": "Test User Story",
                        "description": {
                            "type":"doc",
                            "version":1,
                            "content":[{
                                "type":"paragraph",
                                "content":[{
                                    "type":"text",
                                    "text":"### Refgoals for developers."
                                }]
                            }]
                           },
                        "labels": ["AI-Agent"],
                        "customfield_10100": "AI01 - Waiting for refinement"
                      }
                    }
                  ]
                }
                """;

        JiraIssueDto.SearchResult result = objectMapper.readValue(json, JiraIssueDto.SearchResult.class);
        assertEquals(1, result.getIssues().size());
        assertEquals("TEST-1", result.getIssues().get(0).getKey());
    }

    @Test
    void testCopilotResponseDeserialization() throws Exception {
        String json = """
                {
                  "id": "chatcmpl-abc123",
                  "object": "chat.completion",
                  "created": 1677858242,
                  "model": "gpt-4",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Refined user story: As a user, I want to securely login..."
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 200,
                    "total_tokens": 300
                  }
                }
                """;

        ca.eps_consulting.ig_jira_copilot.dto.CopilotResponseDto response =
                objectMapper.readValue(json, ca.eps_consulting.ig_jira_copilot.dto.CopilotResponseDto.class);

        assertEquals("chatcmpl-abc123", response.getId());
        assertEquals(1, response.getChoices().size());
        assert response.getFirstChoiceContent().contains("Refined user story");
        assertEquals(300, response.getUsage().getTotalTokens());
    }
}
