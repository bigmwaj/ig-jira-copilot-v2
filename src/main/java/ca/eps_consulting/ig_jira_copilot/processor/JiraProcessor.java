package ca.eps_consulting.ig_jira_copilot.processor;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.config.AppConstant;
import ca.eps_consulting.ig_jira_copilot.dto.AtlassianDocumentDto;
import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.dto.JiraTaskDto;
import ca.eps_consulting.ig_jira_copilot.routes.AiState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Processor responsible for building Jira API request payloads and handling Jira-specific logic.
 */
@Component
public class JiraProcessor {

    private static final Logger log = LoggerFactory.getLogger(JiraProcessor.class);

    public static final String HEADER_JIRA_ISSUE_KEY = "JiraIssueKey";
    public static final String HEADER_JIRA_ISSUE = "JiraIssue";
    public static final String HEADER_AI_STATE = "AiState";

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    public JiraProcessor(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the Authorization header value for Jira Basic Auth.
     */
    public String buildBasicAuthHeader() {
        String credentials = appConfig.getJira().getUsername() + ":" + appConfig.getJira().getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public String buildJqlQuery(String aiState) {
        return buildJqlQuery(aiState, AppConstant.JIRA_ISSUE_TYPE_STORY);
    }

    public String buildJqlQuery(String aiState, String jiraIssueType) {
        return "issueType = \"%s\" AND labels = \"%s\" AND cf[%s] = \"%s\" ORDER BY created ASC"
                .formatted(
                        jiraIssueType,
                        appConfig.getJira().getAiAgentLabel(),
                        extractFieldNumber(appConfig.getJira().getAiExchangeTrackingFieldId()),
                        aiState
                );
    }

    private String extractFieldNumber(String fieldId) {
        // customfield_10100 → 10100
        if (fieldId.startsWith("customfield_")) {
            return fieldId.substring("customfield_".length());
        }
        return fieldId;
    }

    public void prepareJiraSearch(Exchange exchange, String jqlQuery) throws Exception {
        String[] fields = {
                "summary",
                "description",
                "labels",
                "issueType",
                "status",
                appConfig.getJira().getAiExchangeTrackingFieldId(),
        };

        String jql = java.net.URLEncoder.encode(jqlQuery, StandardCharsets.UTF_8)
                + "&maxResults=50&fields=" + String.join(",", fields);

        exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.GET);
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "jql=" + jql);
        exchange.getIn().setHeader("Authorization", buildBasicAuthHeader());
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setBody(null);
        log.debug("Prepared Jira search with JQL: {}", jqlQuery);
    }

    public void buildStoryAiStateUpdatePayload(Exchange exchange, String newState) throws Exception {
        JiraIssueDto issue = exchange.getIn().getBody(JiraIssueDto.class);
        Objects.requireNonNull(issue, "JiraIssueDto is null");
        Objects.requireNonNull(issue.getKey(), "JiraIssueDto key is null");

        String issueKey = issue.getKey();
        setJiraHeaders(exchange, issueKey, HttpMethods.PUT.name());
        exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE, issue);
        Map<String, Object> payload = buildAiStateUpdatePayload(AiState.AI02_REFINEMENT_IN_PROGRESS);
        exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
    }

    public Map<String, Object> buildAiStateUpdatePayload(String newState) {
        var aiStateFieldName = appConfig.getJira().getAiExchangeTrackingFieldId();
        return Map.of("fields", Map.of(aiStateFieldName, Map.of("value", newState)));
    }

    /**
     * Builds a Jira issue update payload that updates description and AI Exchange Tracking.
     */
    public Map<String, Object> buildDescriptionAndStateUpdatePayload(String description, String newState) {
        var aiStateFieldName = appConfig.getJira().getAiExchangeTrackingFieldId();
        var content = AtlassianDocumentDto.wrap(description);

        Map<String, Object> fields = new HashMap<>();
        fields.put("description", content);
        fields.put(aiStateFieldName, Map.of("value", newState));
        return Map.of("fields", fields);
    }

    /**
     * Builds a create Jira Task payload linked to a parent user story.
     */
    public Map<String, Object> buildCreateTaskPayload(JiraTaskDto taskDto) {
        Map<String, Object> project = new HashMap<>();
        project.put("key", taskDto.getProjectKey());

        Map<String, Object> issueType = new HashMap<>();
        issueType.put("name", "SubTask");

        Map<String, Object> parent = new HashMap<>();
        parent.put("key", taskDto.getParentIssueKey());

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", project);
        fields.put("issueType", issueType);
        fields.put("summary", taskDto.getSummary());
        fields.put("description", taskDto.getDescription());
        fields.put("parent", parent);
        fields.put(appConfig.getJira().getAiExchangeTrackingFieldId(), taskDto.getAiExchangeTracking());
        fields.put("labels", List.of(appConfig.getJira().getAiAgentLabel()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);
        return payload;
    }

    public void setJiraHeaders(Exchange exchange, String issueKey, String httpMethod) throws Exception {
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, httpMethod);
        exchange.getIn().setHeader("Authorization", buildBasicAuthHeader());
        exchange.getIn().setHeader("Content-Type", "application/json");
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setHeader(HEADER_JIRA_ISSUE_KEY, issueKey);
    }

    public List<JiraIssueDto> extractIssuesFromSearchResult(String responseBody) throws Exception {
        JiraIssueDto.SearchResult result = objectMapper.readValue(responseBody, JiraIssueDto.SearchResult.class);
        log.info("Jira search total returned {} issues", result.getIssues().size());
        log.info("Jira search returned {} issues", result.getIssues());
        return result.getIssues();
    }
}
