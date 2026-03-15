package ca.eps_consulting.ig_jira_copilot.processor;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.dto.CopilotRequestDto;
import ca.eps_consulting.ig_jira_copilot.dto.CopilotResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processor responsible for building Copilot API request payloads and parsing responses.
 */
@Component
public class CopilotProcessor {

    private static final Logger log = LoggerFactory.getLogger(CopilotProcessor.class);

    public static final String HEADER_COPILOT_PROMPT = "CopilotPrompt";

    public static final String HEADER_AI_RESPONSE = "AiResponse";

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    public CopilotProcessor(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Prepares an exchange to call the Copilot chat completions API.
     *
     * @param exchange the Camel exchange
     * @param prompt   the prompt text to send to the AI
     */
    public void prepareCopilotRequest(Exchange exchange, String prompt) throws Exception {
        AppConfig.Copilot copilotConfig = appConfig.getCopilot();

        CopilotRequestDto request = new CopilotRequestDto(
            copilotConfig.getModel(),
            List.of(
                new CopilotRequestDto.Message("system",
                    "You are an expert software architect and developer. Provide clear, structured, and actionable responses."),
                new CopilotRequestDto.Message("user", prompt)
            ),
            copilotConfig.getMaxTokens()
        );

        String requestBody = objectMapper.writeValueAsString(request);

        var jiraIssueKey = exchange.getIn().getHeader("JiraIssueKey");
        exchange.getIn().getHeaders().clear();
        exchange.getIn().setHeader("JiraIssueKey", jiraIssueKey);

        exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
        exchange.getIn().setHeader("Authorization", "Bearer " + copilotConfig.getApiKey());
        exchange.getIn().setHeader("Content-Type", "application/json");
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setBody(requestBody);

        log.debug("Prepared Copilot request for prompt (first 100 chars): {}",
            prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);
    }

    /**
     * Parses the Copilot API response body and extracts the generated content.
     *
     * @param responseBody raw JSON response from Copilot API
     * @return extracted AI content string
     */
    public String parseCopilotResponse(String responseBody) throws Exception {
        CopilotResponseDto response = objectMapper.readValue(responseBody, CopilotResponseDto.class);
        String content = response.getFirstChoiceContent();
        log.info("Received Copilot response with {} tokens", response.getUsage() != null ? response.getUsage().getTotalTokens() : "unknown");
        return content;
    }

    /**
     * Builds a user story refinement prompt.
     */
    public String buildRefinementPrompt(String summary, String description) {
        return String.format(
            """
            Please refine the following Jira user story to make it clearer, more actionable, and complete.
            Follow the format: As a [user], I want [goal] so that [benefit].
            Include acceptance criteria.

            Summary: %s

            Description:
            %s

            Provide the refined user story with acceptance criteria in a structured format.
            The response should be in Atlassian Document Format and should be translated into french.
            """,
            summary,
            description != null ? description : "(no description provided)"
        );
    }

    /**
     * Builds a development plan generation prompt.
     */
    public String buildDevPlanPrompt(String summary, String description) {
        return String.format(
            """
            Generate a detailed development plan for the following user story.
            Include: technical approach, required components, estimated effort, dependencies, and test strategy.

            Summary: %s

            Description:
            %s

            Provide a structured development plan.
            """,
            summary,
            description != null ? description : "(no description provided)"
        );
    }

    /**
     * Builds a development plan review prompt.
     */
    public String buildDevPlanReviewPrompt(String summary, String description) {
        return String.format(
            """
            Review the following development plan for a Jira task.
            Identify potential risks, missing considerations, improvements, and best practices.

            Task Summary: %s

            Development Plan:
            %s

            Provide a reviewed and improved version of the development plan.
            """,
            summary,
            description != null ? description : "(no description provided)"
        );
    }

    /**
     * Builds a code generation prompt.
     */
    public String buildCodeGenPrompt(String summary, String description) {
        return String.format(
            """
            Generate production-ready code based on the following development plan.
            Follow clean code principles, include error handling, logging, and unit tests.

            Task Summary: %s

            Development Plan:
            %s

            Generate the implementation code with inline documentation.
            """,
            summary,
            description != null ? description : "(no description provided)"
        );
    }
}
