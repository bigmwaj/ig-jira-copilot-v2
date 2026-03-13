package ca.eps_consulting.ig_jira_copilot.routes;

import ca.eps_consulting.ig_jira_copilot.config.AppConfig;
import ca.eps_consulting.ig_jira_copilot.processor.CopilotProcessor;
import ca.eps_consulting.ig_jira_copilot.processor.JiraProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for all AI orchestration Camel routes.
 * Provides shared autowired dependencies for Jira and Copilot integration.
 */
public abstract class BaseOrchestrationRoute extends RouteBuilder {

    @Autowired
    protected AppConfig appConfig;

    @Autowired
    protected JiraProcessor jiraProcessor;

    @Autowired
    protected CopilotProcessor copilotProcessor;

    @Autowired
    protected ObjectMapper objectMapper;
}
