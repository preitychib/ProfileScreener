package com.team.resume.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.resumes")
public record ResumeProperties(String directory) {
}
