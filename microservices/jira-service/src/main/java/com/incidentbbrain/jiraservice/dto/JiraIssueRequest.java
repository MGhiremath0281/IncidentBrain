package com.incidentbbrain.jiraservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JiraIssueRequest {
    private Fields fields;

    @Data
    @Builder
    public static class Fields {
        private Project project;
        private String summary;
        private String description;
        private IssueType issuetype;
        private List<String> labels;
    }

    @Data
    @Builder
    public static class Project {
        private String key;
    }

    @Data
    @Builder
    public static class IssueType {
        private String name;
    }
}