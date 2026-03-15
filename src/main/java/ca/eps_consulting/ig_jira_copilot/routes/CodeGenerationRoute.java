package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.config.AppConstant;
import ca.eps_consulting.ig_jira_copilot.dto.JiraIssueDto;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

//@Component
public class CodeGenerationRoute extends BaseOrchestrationRoute {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationRoute.class);

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
        // ROUTE 4a: Timer-triggered Jira polling for AI10 tasks
        // ----------------------------------------------------------------
        from("timer:codeGenPoller?period={{app.routes.code-gen-schedule}}")
            .routeId("route-codegen-poll")
            .log(LoggingLevel.INFO, log, "Route 4: Polling Jira for Tasks awaiting code generation [AI10]")
            .process(exchange -> {
                String jql = jiraProcessor.buildJqlQuery(AiState.AI10_WAITING_CODE_GEN, AppConstant.JIRA_ISSUE_TYPE_SUB_TASK);
                jiraProcessor.prepareJiraSearch(exchange, jql);
            })
            .toD("{{camel.http.jira-search-uri}}?throwExceptionOnFailure=true")
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                List<JiraIssueDto> issues = jiraProcessor.extractIssuesFromSearchResult(body);
                exchange.getIn().setBody(issues);
            })
            .split(body()).parallelProcessing()
                .log(LoggingLevel.INFO, log, "Route 4: Processing task ${body.key} for code generation")
                .to("seda:codegen-process?waitForTaskToComplete=Never")
            .end();

        // ----------------------------------------------------------------
        // ROUTE 4b: Asynchronous processing of each task
        // ----------------------------------------------------------------
        from("seda:codegen-process?concurrentConsumers=5")
            .routeId("route-codegen-process")
            .log(LoggingLevel.INFO, log, "Route 4: Starting code generation for task ${body.key}")
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getBody(JiraIssueDto.class);
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, issue.getKey());
                exchange.getIn().setHeader(JiraProcessor.HEADER_JIRA_ISSUE, issue);
            })
            // Step 1: Update task to AI11 (Code generation in progress)
            .process(exchange -> {
                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildAiStateUpdatePayload(AiState.AI11_CODE_GEN_IN_PROGRESS);
                jiraProcessor.setJiraHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 4: Updated task ${header.JiraIssueKey} to AI11")
            // Step 2: Call Copilot API to generate code
            .process(exchange -> {
                JiraIssueDto issue = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE, JiraIssueDto.class);
                String prompt = copilotProcessor.buildCodeGenPrompt(
                    issue.getFields().getSummary(),
                    issue.getFields().getDescription().getContentString()
                );
                copilotProcessor.prepareCopilotRequest(exchange, prompt);
            })
            .toD("{{camel.http.copilot-completions-uri}}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, log, "Route 4: Received generated code from Copilot for ${header.JiraIssueKey}")
            // Step 3: Update task to AI12 (Code generation completed)
            .process(exchange -> {
                String responseBody = exchange.getIn().getBody(String.class);
                String aiContent = copilotProcessor.parseCopilotResponse(responseBody);
                exchange.getIn().setHeader(CopilotProcessor.HEADER_AI_RESPONSE, aiContent);

                String issueKey = exchange.getIn().getHeader(JiraProcessor.HEADER_JIRA_ISSUE_KEY, String.class);
                Map<String, Object> payload = jiraProcessor.buildDescriptionAndStateUpdatePayload(
                    aiContent,AiState.AI12_CODE_GEN_COMPLETED
                );
                jiraProcessor.setJiraHeaders(exchange, issueKey, HttpMethods.PUT.name());
                exchange.getIn().setBody(objectMapper.writeValueAsString(payload));
            })
            .toD("{{camel.http.jira-issue-uri}}/${header.JiraIssueKey}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, log, "Route 4: Completed code generation for task ${header.JiraIssueKey} → AI12")
            .onException(Exception.class)
                .log(LoggingLevel.ERROR, log, "Route 4: Error processing task ${header.JiraIssueKey}: ${exception.message}")
                .handled(true)
            .end();
    }
}
