package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO representing a response from the Copilot (OpenAI-compatible) API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopilotResponseDto {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    /**
     * Extracts the text content from the first choice, if available.
     *
     * @return AI-generated text or empty string
     */
    public String getFirstChoiceContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice first = choices.get(0);
            if (first.getMessage() != null) {
                return first.getMessage().getContent();
            }
        }
        return "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Choice {
        private int index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
