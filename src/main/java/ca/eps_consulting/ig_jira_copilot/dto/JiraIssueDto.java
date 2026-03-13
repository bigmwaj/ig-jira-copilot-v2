package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Jira issue retrieved from the REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueDto {

    private String id;
    private String key;
    private Fields fields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {

        private String summary;
        private String description;
        private List<String> labels;

        @JsonProperty("customfield_10100")
        private String aiExchangeTracking;

        private IssueType issuetype;
        private Map<String, Object> status;

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

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public String getAiExchangeTracking() {
            return aiExchangeTracking;
        }

        public void setAiExchangeTracking(String aiExchangeTracking) {
            this.aiExchangeTracking = aiExchangeTracking;
        }

        public IssueType getIssuetype() {
            return issuetype;
        }

        public void setIssuetype(IssueType issuetype) {
            this.issuetype = issuetype;
        }

        public Map<String, Object> getStatus() {
            return status;
        }

        public void setStatus(Map<String, Object> status) {
            this.status = status;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * DTO for Jira search results.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        private int total;
        private int startAt;
        private int maxResults;
        private List<JiraIssueDto> issues;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getStartAt() {
            return startAt;
        }

        public void setStartAt(int startAt) {
            this.startAt = startAt;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public List<JiraIssueDto> getIssues() {
            return issues;
        }

        public void setIssues(List<JiraIssueDto> issues) {
            this.issues = issues;
        }
    }
}
