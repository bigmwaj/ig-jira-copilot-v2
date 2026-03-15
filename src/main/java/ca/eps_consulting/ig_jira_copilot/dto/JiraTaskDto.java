package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Jira Task to be created and linked to a user story.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraTaskDto {
    private String summary;
    private String description;
    private String parentIssueKey;
    private String projectKey;
    private String aiExchangeTracking;

    public JiraTaskDto(String summary, String description, String parentIssueKey, String projectKey) {
        this.summary = summary;
        this.description = description;
        this.parentIssueKey = parentIssueKey;
        this.projectKey = projectKey;
    }
}
