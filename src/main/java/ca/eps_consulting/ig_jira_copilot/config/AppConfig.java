package ca.eps_consulting.ig_jira_copilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration properties for Jira and Copilot API endpoints.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {

    private Jira jira = new Jira();
    private Copilot copilot = new Copilot();
    private Routes routes = new Routes();

    @Data
    public static class Jira {
        private String baseUrl = "https://your-jira-instance.atlassian.net";
        private String username;
        private String apiToken;
        private String projectKey;
        private String aiExchangeTrackingFieldId = "customfield_10100";
        private String aiAgentLabel = "AI-Agent";
    }

    @Data
    public static class Copilot {
        private String apiUrl = "https://api.githubcopilot.com";
        private String apiKey;
        private String model = "gpt-4o";
        private int maxTokens = 4096;
        private int pollingIntervalMs = 5000;
        private int maxPollingAttempts = 60;
    }

    @Data
    public static class Routes {
        private String refinementSchedule = "60000";
        private String devPlanSchedule = "60000";
        private String devPlanReviewSchedule = "60000";
        private String codeGenSchedule = "60000";
    }
}
