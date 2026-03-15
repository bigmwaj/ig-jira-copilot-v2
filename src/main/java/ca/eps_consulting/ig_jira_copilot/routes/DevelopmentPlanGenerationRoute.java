package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.config.AppConstant;
import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.dto.JiraTaskDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

//@Component
public class DevelopmentPlanGenerationRoute extends BaseOrchestrationRoute {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentPlanGenerationRoute.class);

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
        // ROUTE 2a: Timer-triggered Jira polling for [AI04] issues
        // ----------------------------------------------------------------
        from("timer:devPlanPoller?period={{app.routes.dev-plan-schedule}}")
            .routeId("route-devplan-poll")
            .log(LoggingLevel.INFO, log, "Route 2: Polling Jira for User Stories awaiting dev plan ([AI04])")
            .process(exchange -> {
                String jql = jiraProcessor.buildJqlQuery(AiState.AI04_WAITING_DEV_PLAN, AppConstant.JIRA_ISSUE_TYPE_STORY);
                jiraProcessor.prepareJiraSearch(exchange, jql);
            })
            .toD("{{camel.http.jira-search-uri}}?throwExceptionOnFailure=true")
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                List<JiraIssueDto> issues = jiraProcessor.extractIssuesFromSearchResult(body);
                exchange.getIn().setBody(issues);
            })
            .split(body()).parallelProcessing()
                .log(LoggingLevel.INFO, log, "Route 2: Processing issue ${body.key} for dev plan generation")
                .to("seda:devplan-process?waitForTaskToComplete=Never")
            .end();

        // ----------------------------------------------------------------
        // ROUTE 2b: Asynchronous processing of each story
        // ----------------------------------------------------------------
        from("seda:devplan-process?concurrentConsumers=5")
            .routeId("route-devplan-process")
            .log(LoggingLevel.INFO, log, "Route 2: Starting dev plan generation for issue ${body.key}")
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getBody(JiraIssueDto.class);
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, issue.getKey());
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE, issue);
            })
            // Step 1: Update Jira to [AI05] (Dev plan generation in progress)
            .process(exchange -> {
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload(AiState.AI05_DEV_PLAN_IN_PROGRESS);
                jiraProcessor.setJiraHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 2: Updated issue ${header.JiraIssueKey} to AI05")
            // Step 2: Call Copilot API to generate development plan
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String prompt = copilotProcessor.buildDevPlanPrompt(
                    issue.getFields().getSummary(),
                    issue.getFields().getDescription().getContentString()
                );
                copilotProcessor.prepareCopilotRequest(exchange, prompt);
            })
            .toD("{{camel.http.copilot-completions-uri}}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, log, "Route 2: Received dev plan from Copilot for ${header.JiraIssueKey}")
            // Step 3: Update Jira to AI06 and create a linked Task
            .process(exchange -> {
                String responseBody = exchange.getIn().getBody(String.class);
                String aiContent = copilotProcessor.parseCopilotResponse(responseBody);
                exchange.getIn().setHeader(CopilotProcessor.HEADER_AI_RESPONSE, aiContent);

                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);

                // Update parent story to AI06
                Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload(AiState.AI06_DEV_PLAN_GENERATED);
                jiraProcessor.setJiraHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 2: Updated issue ${header.JiraIssueKey} to AI06")
            // Step 4: Create a Jira Task linked to the user story
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                String aiContent = exchange.getIn().getHeader(CopilotProcessor.HEADER_AI_RESPONSE, String.class);

                String projectKey = appConfig.getJira().getProjectKey() != null
                    ? appConfig.getJira().getProjectKey()
                    : issueKey.split("-")[0];

                JiraTaskDto task = new JiraTaskDto(
                    "Development Plan: " + issue.getFields().getSummary(),
                    aiContent,
                    issueKey,
                    projectKey
                );
                task.setAiExchangeTracking(AiState.AI07_WAITING_DEV_PLAN_REVIEW);

                Map<String, Object> taskPayload = jiraProcessor.buildCreateTaskPayload(task);
                jiraProcessor.setJiraHeaders(exchange, issueKey, HttpMethods.POST.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(taskPayload));
            })
            .toD("{{camel.http.jira-create-issue-uri}}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 2: Created Jira Task linked to ${header.JiraIssueKey}")
            .onException(Exception.class)
                .log(LoggingLevel.ERROR, log, "Route 2: Error processing issue ${header.JiraIssueKey}: ${exception.message}")
                .handled(true)
            .end();
    }
}
