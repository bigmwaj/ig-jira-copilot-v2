package ca.eps_consulting.ig_jira_copilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration properties for Jira and Copilot API endpoints.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Jira jira = new Jira();
    private Copilot copilot = new Copilot();
    private Routes routes = new Routes();

    public Jira getJira() {
        return jira;
    }

    public void setJira(Jira jira) {
        this.jira = jira;
    }

    public Copilot getCopilot() {
        return copilot;
    }

    public void setCopilot(Copilot copilot) {
        this.copilot = copilot;
    }

    public Routes getRoutes() {
        return routes;
    }

    public void setRoutes(Routes routes) {
        this.routes = routes;
    }

    public static class Jira {
        private String baseUrl = "https://your-jira-instance.atlassian.net";
        private String username;
        private String apiToken;
        private String projectKey;
        private String aiExchangeTrackingFieldId = "customfield_10100";
        private String aiAgentLabel = "AI-Agent";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        public String getAiExchangeTrackingFieldId() {
            return aiExchangeTrackingFieldId;
        }

        public void setAiExchangeTrackingFieldId(String aiExchangeTrackingFieldId) {
            this.aiExchangeTrackingFieldId = aiExchangeTrackingFieldId;
        }

        public String getAiAgentLabel() {
            return aiAgentLabel;
        }

        public void setAiAgentLabel(String aiAgentLabel) {
            this.aiAgentLabel = aiAgentLabel;
        }
    }

    public static class Copilot {
        private String apiUrl = "https://api.githubcopilot.com";
        private String apiKey;
        private String model = "gpt-4";
        private int maxTokens = 4096;
        private int pollingIntervalMs = 5000;
        private int maxPollingAttempts = 60;

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getPollingIntervalMs() {
            return pollingIntervalMs;
        }

        public void setPollingIntervalMs(int pollingIntervalMs) {
            this.pollingIntervalMs = pollingIntervalMs;
        }

        public int getMaxPollingAttempts() {
            return maxPollingAttempts;
        }

        public void setMaxPollingAttempts(int maxPollingAttempts) {
            this.maxPollingAttempts = maxPollingAttempts;
        }
    }

    public static class Routes {
        private String refinementSchedule = "60000";
        private String devPlanSchedule = "60000";
        private String devPlanReviewSchedule = "60000";
        private String codeGenSchedule = "60000";

        public String getRefinementSchedule() {
            return refinementSchedule;
        }

        public void setRefinementSchedule(String refinementSchedule) {
            this.refinementSchedule = refinementSchedule;
        }

        public String getDevPlanSchedule() {
            return devPlanSchedule;
        }

        public void setDevPlanSchedule(String devPlanSchedule) {
            this.devPlanSchedule = devPlanSchedule;
        }

        public String getDevPlanReviewSchedule() {
            return devPlanReviewSchedule;
        }

        public void setDevPlanReviewSchedule(String devPlanReviewSchedule) {
            this.devPlanReviewSchedule = devPlanReviewSchedule;
        }

        public String getCodeGenSchedule() {
            return codeGenSchedule;
        }

        public void setCodeGenSchedule(String codeGenSchedule) {
            this.codeGenSchedule = codeGenSchedule;
        }
    }
}
