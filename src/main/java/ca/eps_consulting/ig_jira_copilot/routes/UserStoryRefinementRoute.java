package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Route 1: User Story Refinement
 *
 * <p>Polls Jira for issues with AI Exchange Tracking starting with "AI01" (Waiting for refinement),
 * sends each story to the Copilot API for refinement, and updates the issue with the AI response.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Timer triggers polling of Jira (AI01 → AI02)</li>
 *   <li>Each issue is sent asynchronously to SEDA for processing</li>
 *   <li>Copilot API is called with a refinement prompt</li>
 *   <li>On response, Jira description is updated and state set to AI03</li>
 * </ol>
 */
@Component
public class UserStoryRefinementRoute extends BaseOrchestrationRoute {

    private static final Logger log = LoggerFactory.getLogger(UserStoryRefinementRoute.class);

    @Override
    public void configure() {
        // Global error handler with retry and exponential backoff
        errorHandler(deadLetterChannel("log:ca.eps_consulting.dead-letter?level=ERROR")
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .backOffMultiplier(2)
            .useExponentialBackOff()
            .logRetryAttempted(true)
            .logExhausted(true));

        // ----------------------------------------------------------------
        // ROUTE 1a: Timer-triggered Jira polling for AI01 issues
        // ----------------------------------------------------------------
        from("timer:refinementPoller?period={{app.routes.refinement-schedule}}")
            .routeId("route-refinement-poll")
            .log(LoggingLevel.INFO, log, "Route 1: Polling Jira for User Stories awaiting refinement (AI01)")
            .process(exchange -> {
                String jql = jiraProcessor.buildJqlQuery("AI01");
                jiraProcessor.prepareJiraSearch(exchange, jql);
            })
            .toD("{{camel.http.jira-search-uri}}?throwExceptionOnFailure=true")
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                List<JiraIssueDto> issues = jiraProcessor.extractIssuesFromSearchResult(body);
                exchange.getIn().setBody(issues);
            })
            .split(body()).parallelProcessing()
                .log(LoggingLevel.INFO, log, "Route 1: Processing issue ${body.key} for refinement")
                .to("seda:refinement-process?waitForTaskToComplete=Never")
            .end();

        // ----------------------------------------------------------------
        // ROUTE 1b: Asynchronous processing of each story
        // ----------------------------------------------------------------
        from("seda:refinement-process?concurrentConsumers=5")
            .routeId("route-refinement-process")
            .log(LoggingLevel.INFO, log, "Route 1: Starting refinement for issue ${body.key}")
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getBody(JiraIssueDto.class);
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, issue.getKey());
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE, issue);
            })
            // Step 1: Update Jira to AI02 (Refinement in progress)
            .process(exchange -> {
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload("AI02 - Refinement in progress");
                jiraProcessor.setJiraWriteHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 1: Updated issue ${header.JiraIssueKey} to AI02")
            // Step 2: Call Copilot API for refinement
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String prompt = copilotProcessor.buildRefinementPrompt(
                    issue.getFields().getSummary(),
                    issue.getFields().getDescription()
                );
                copilotProcessor.prepareCopilotRequest(exchange, prompt);
            })
            .toD("{{camel.http.copilot-completions-uri}}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, log, "Route 1: Received Copilot refinement response for ${header.JiraIssueKey}")
            // Step 3: Parse AI response and update Jira to AI03
            .process(exchange -> {
                String responseBody = exchange.getIn().getBody(String.class);
                String aiContent = copilotProcessor.parseCopilotResponse(responseBody);
                exchange.getIn().setHeader(CopilotProcessor.HEADER_AI_RESPONSE, aiContent);

                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildDescriptionAndStateUpdatePayload(
                    aiContent, "AI03 - Refinement completed"
                );
                jiraProcessor.setJiraWriteHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 1: Completed refinement for issue ${header.JiraIssueKey} → AI03")
            .onException(Exception.class)
                .log(LoggingLevel.ERROR, log, "Route 1: Error processing issue ${header.JiraIssueKey}: ${exception.message}")
                .handled(true)
            .end();
    }
}
