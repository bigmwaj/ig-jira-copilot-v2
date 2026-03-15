package ca.eps_consulting.ig_jira_copilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class IgJiraCopilotApplication {

    private static final Logger log = LoggerFactory.getLogger(IgJiraCopilotApplication.class);

    public static void main(String[] args) {
        log.info("Starting IG Jira Copilot AI Orchestration Service...");
        SpringApplication.run(IgJiraCopilotApplication.class, args);
    }
}
