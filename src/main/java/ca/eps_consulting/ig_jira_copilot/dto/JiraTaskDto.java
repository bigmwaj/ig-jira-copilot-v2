package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a Jira Task to be created and linked to a user story.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraTaskDto {

    private String summary;
    private String description;
    private String parentIssueKey;
    private String projectKey;
    private String aiExchangeTracking;

    public JiraTaskDto() {}

    public JiraTaskDto(String summary, String description, String parentIssueKey, String projectKey) {
        this.summary = summary;
        this.description = description;
        this.parentIssueKey = parentIssueKey;
        this.projectKey = projectKey;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentIssueKey() {
        return parentIssueKey;
    }

    public void setParentIssueKey(String parentIssueKey) {
        this.parentIssueKey = parentIssueKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getAiExchangeTracking() {
        return aiExchangeTracking;
    }

    public void setAiExchangeTracking(String aiExchangeTracking) {
        this.aiExchangeTracking = aiExchangeTracking;
    }
}
