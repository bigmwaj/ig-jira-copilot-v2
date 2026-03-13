package ca.eps_consulting.ig_jira_copilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the IG Jira Copilot AI Orchestration Service.
 *
 * <p>This application integrates Jira and GitHub Copilot via Apache Camel routes to automate
 * the software development lifecycle. It provides four orchestration workflows:
 * <ol>
 *   <li>User Story Refinement (AI01 → AI03)</li>
 *   <li>Development Plan Generation (AI04 → AI06)</li>
 *   <li>Development Plan Review (AI07 → AI09)</li>
 *   <li>Code Generation (AI10 → AI12)</li>
 * </ol>
 */
@SpringBootApplication
@EnableConfigurationProperties
public class IgJiraCopilotApplication {

    private static final Logger log = LoggerFactory.getLogger(IgJiraCopilotApplication.class);

    public static void main(String[] args) {
        log.info("Starting IG Jira Copilot AI Orchestration Service...");
        SpringApplication.run(IgJiraCopilotApplication.class, args);
    }
}
