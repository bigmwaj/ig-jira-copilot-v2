package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Route 3: Development Plan Review
 *
 * <p>Polls Jira for Tasks with AI Exchange Tracking starting with "AI07" (Waiting for dev plan review),
 * sends each task's plan to Copilot for review, and updates the task with the reviewed plan.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Timer triggers polling of Jira (AI07 → AI08)</li>
 *   <li>Each task is sent asynchronously to SEDA</li>
 *   <li>Copilot API reviews the development plan</li>
 *   <li>Task description updated and state set to AI09</li>
 * </ol>
 */
@Component
public class DevelopmentPlanReviewRoute extends BaseOrchestrationRoute {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentPlanReviewRoute.class);

    @Override
    public void configure() {
        errorHandler(deadLetterChannel("log:ca.eps_consulting.dead-letter?level=ERROR")
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .backOffMultiplier(2)
            .useExponentialBackOff()
            .logRetryAttempted(true)
            .logExhausted(true));

        // ----------------------------------------------------------------
        // ROUTE 3a: Timer-triggered Jira polling for AI07 tasks
        // ----------------------------------------------------------------
        from("timer:devPlanReviewPoller?period={{app.routes.dev-plan-review-schedule}}")
            .routeId("route-devplan-review-poll")
            .log(LoggingLevel.INFO, log, "Route 3: Polling Jira for Tasks awaiting dev plan review (AI07)")
            .process(exchange -> {
                String jql = jiraProcessor.buildJqlQuery("AI07");
                jiraProcessor.prepareJiraSearch(exchange, jql);
            })
            .toD("{{camel.http.jira-search-uri}}?throwExceptionOnFailure=true")
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                List<JiraIssueDto> issues = jiraProcessor.extractIssuesFromSearchResult(body);
                exchange.getIn().setBody(issues);
            })
            .split(body()).parallelProcessing()
                .log(LoggingLevel.INFO, log, "Route 3: Processing task ${body.key} for dev plan review")
                .to("seda:devplan-review-process?waitForTaskToComplete=Never")
            .end();

        // ----------------------------------------------------------------
        // ROUTE 3b: Asynchronous processing of each task
        // ----------------------------------------------------------------
        from("seda:devplan-review-process?concurrentConsumers=5")
            .routeId("route-devplan-review-process")
            .log(LoggingLevel.INFO, log, "Route 3: Starting dev plan review for task ${body.key}")
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getBody(JiraIssueDto.class);
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, issue.getKey());
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE, issue);
            })
            // Step 1: Update task to AI08 (Review in progress)
            .process(exchange -> {
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload("AI08 - Review in progress");
                jiraProcessor.setJiraWriteHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 3: Updated task ${header.JiraIssueKey} to AI08")
            // Step 2: Call Copilot to review the development plan
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String prompt = copilotProcessor.buildDevPlanReviewPrompt(
                    issue.getFields().getSummary(),
                    issue.getFields().getDescription()
                );
                copilotProcessor.prepareCopilotRequest(exchange, prompt);
            })
            .toD("{{camel.http.copilot-completions-uri}}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, log, "Route 3: Received review from Copilot for ${header.JiraIssueKey}")
            // Step 3: Update task description with reviewed plan and set to AI09
            .process(exchange -> {
                String responseBody = exchange.getIn().getBody(String.class);
                String aiContent = copilotProcessor.parseCopilotResponse(responseBody);
                exchange.getIn().setHeader(CopilotProcessor.HEADER_AI_RESPONSE, aiContent);

                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildDescriptionAndStateUpdatePayload(
                    aiContent, "AI09 - Review completed"
                );
                jiraProcessor.setJiraWriteHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 3: Completed review for task ${header.JiraIssueKey} → AI09")
            .onException(Exception.class)
                .log(LoggingLevel.ERROR, log, "Route 3: Error processing task ${header.JiraIssueKey}: ${exception.message}")
                .handled(true)
            .end();
    }
}
