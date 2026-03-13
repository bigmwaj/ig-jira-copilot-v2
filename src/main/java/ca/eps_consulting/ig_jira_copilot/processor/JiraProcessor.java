package ca.eps_consulting.ig_jira_copilot.processor;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.dto.JiraTaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor responsible for building Jira API request payloads and handling Jira-specific logic.
 */
@Component
public class JiraProcessor {

    private static final Logger log = LoggerFactory.getLogger(JiraProcessor.class);

    public static final String HEADER_JIRA_ISSUE_KEY = "JiraIssueKey";
    public static final String HEADER_JIRA_ISSUE = "JiraIssue";
    public static final String HEADER_AI_STATE = "AiState";

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Builds the Authorization header value for Jira Basic Auth.
     */
    public String buildBasicAuthHeader() {
        String credentials = appConfig.getJira().getUsername() + ":" + appConfig.getJira().getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Builds a JQL query string to find issues matching an AI Exchange Tracking prefix and label.
     *
     * @param aiStatePrefix the AI state prefix (e.g., "AI01")
     * @return JQL query string
     */
    public String buildJqlQuery(String aiStatePrefix) {
        return String.format(
            "labels = \"%s\" AND cf[%s] ~ \"%s*\" ORDER BY created ASC",
            appConfig.getJira().getAiAgentLabel(),
            extractFieldNumber(appConfig.getJira().getAiExchangeTrackingFieldId()),
            aiStatePrefix
        );
    }

    private String extractFieldNumber(String fieldId) {
        // customfield_10100 → 10100
        if (fieldId.startsWith("customfield_")) {
            return fieldId.substring("customfield_".length());
        }
        return fieldId;
    }

    /**
     * Prepares the exchange to execute a Jira search via the HTTP component.
     */
    public void prepareJiraSearch(Exchange exchange, String jqlQuery) throws Exception {
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.GET);
        exchange.getIn().setHeader(Exchange.HTTP_QUERY,
            "jql=" + java.net.URLEncoder.encode(jqlQuery, "UTF-8") + "&maxResults=50&fields=summary,description,labels," + appConfig.getJira().getAiExchangeTrackingFieldId() + ",issuetype,status");
        exchange.getIn().setHeader("Authorization", buildBasicAuthHeader());
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setBody(null);
        log.debug("Prepared Jira search with JQL: {}", jqlQuery);
    }

    /**
     * Builds a Jira issue update payload that sets the AI Exchange Tracking field.
     */
    public Map<String, Object> buildAiStateUpdatePayload(String newState) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(appConfig.getJira().getAiExchangeTrackingFieldId(), newState);
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);
        return payload;
    }

    /**
     * Builds a Jira issue update payload that updates description and AI Exchange Tracking.
     */
    public Map<String, Object> buildDescriptionAndStateUpdatePayload(String description, String newState) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("description", description);
        fields.put(appConfig.getJira().getAiExchangeTrackingFieldId(), newState);
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);
        return payload;
    }

    /**
     * Builds a create Jira Task payload linked to a parent user story.
     */
    public Map<String, Object> buildCreateTaskPayload(JiraTaskDto taskDto) {
        Map<String, Object> project = new HashMap<>();
        project.put("key", taskDto.getProjectKey());

        Map<String, Object> issueType = new HashMap<>();
        issueType.put("name", "Task");

        Map<String, Object> parent = new HashMap<>();
        parent.put("key", taskDto.getParentIssueKey());

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", project);
        fields.put("issuetype", issueType);
        fields.put("summary", taskDto.getSummary());
        fields.put("description", taskDto.getDescription());
        fields.put("parent", parent);
        fields.put(appConfig.getJira().getAiExchangeTrackingFieldId(), taskDto.getAiExchangeTracking());
        fields.put("labels", List.of(appConfig.getJira().getAiAgentLabel()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);
        return payload;
    }

    /**
     * Sets standard Jira PUT/POST headers on the exchange.
     */
    public void setJiraWriteHeaders(Exchange exchange, String issueKey, String httpMethod) throws Exception {
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, httpMethod);
        exchange.getIn().setHeader("Authorization", buildBasicAuthHeader());
        exchange.getIn().setHeader("Content-Type", "application/json");
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setHeader(HEADER_JIRA_ISSUE_KEY, issueKey);
    }

    /**
     * Extracts the list of Jira issues from a search result.
     */
    public List<JiraIssueDto> extractIssuesFromSearchResult(String responseBody) throws Exception {
        JiraIssueDto.SearchResult result = objectMapper.readValue(responseBody, JiraIssueDto.SearchResult.class);
        log.info("Jira search returned {} issues", result.getTotal());
        return result.getIssues();
    }
}
