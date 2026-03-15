package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Jira issue retrieved from the REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class JiraIssueDto {

    private String id;
    private String key;
    private Fields fields;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {

        private String summary;

        private AtlassianDocumentDto description;

        private List<String> labels;

        @JsonProperty("customfield_10100")
        private String aiExchangeTracking;

        private IssueType issuetype;

        private Map<String, Object> status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueType {
        private String name;
    }

    /**
     * DTO for Jira search results.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
//        private int total;
//        private int startAt;
//        private int maxResults;
        private List<JiraIssueDto> issues;
    }
}
