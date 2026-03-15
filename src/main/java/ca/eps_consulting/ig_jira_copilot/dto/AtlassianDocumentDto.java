package ca.eps_consulting.ig_jira_copilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder(setterPrefix = "with")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AtlassianDocumentDto {

    public static final String CONTENT_TYPE_DOC = "doc";

    public static final String CONTENT_TYPE_TEXT = "text";

    public static final String CONTENT_TYPE_PARAGRAPH = "paragraph";

    public static final String CONTENT_TYPE_HEADING = "heading";

    private String type;

    private Integer version;

    private String text;

    @JsonProperty("content")
    @Singular
    private List<AtlassianDocumentDto> contents;

    @JsonIgnore
    public String getContentString() {
        if (contents == null) {
            return null;
        }
        return contents.stream().map(AtlassianDocumentDto::getText).collect(Collectors.joining(" "));
    }

    public static AtlassianDocumentDto wrap(String docText) {
        return AtlassianDocumentDto.builder()
                .withType(AtlassianDocumentDto.CONTENT_TYPE_DOC)
                .withVersion(1)
                .withContent(
                        AtlassianDocumentDto.builder()
                                .withType(AtlassianDocumentDto.CONTENT_TYPE_PARAGRAPH)
                                .withContent(AtlassianDocumentDto.builder().withType(CONTENT_TYPE_TEXT).withText(docText).build())
                                .build()
                )
                .build();
    }
}
