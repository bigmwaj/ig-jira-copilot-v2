package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending a request to the Copilot (OpenAI-compatible) API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotRequestDto {

    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature = 0.7;

    public CopilotRequestDto(String model, List<Message> messages, int maxTokens) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = maxTokens;
    }

    /**
     * Represents a single chat message in the request.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
