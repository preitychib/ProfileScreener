package com.team.resume.rag.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateQueryParserTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final CandidateQueryParser parser = new CandidateQueryParser(chatModel, new ObjectMapper());

    @Test
    void parsesTechnologyConjunction() {
        when(chatModel.chat(anyString())).thenReturn("""
                {
                  "requiredTechnologies": ["AWS", "Spring Boot"],
                  "minimumYearsExperience": null,
                  "experienceInclusive": false,
                  "minimumGraduationCgpa": null,
                  "cgpaInclusive": false,
                  "minimumGraduationPercentage": null,
                  "percentageInclusive": false,
                  "educationScoreOperator": "ANY"
                }
                """);

        CandidateQueryCriteria criteria =
                parser.parse("Name candidates having experience in AWS and Spring Boot");

        assertThat(criteria.requiredTechnologies()).containsExactly("AWS", "Spring Boot");
        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    void parsesExclusiveExperienceAndAlternativeEducationScores() {
        when(chatModel.chat(anyString())).thenReturn("""
                ```json
                {
                  "requiredTechnologies": [],
                  "minimumYearsExperience": 3,
                  "experienceInclusive": false,
                  "minimumGraduationCgpa": 7.5,
                  "cgpaInclusive": false,
                  "minimumGraduationPercentage": 75.0,
                  "percentageInclusive": false,
                  "educationScoreOperator": "ANY"
                }
                ```
                """);

        CandidateQueryCriteria criteria = parser.parse(
                "Candidates with more than 3 years and CGPA above 7.5 or graduation percentage above 75"
        );

        assertThat(criteria.minimumYearsExperience()).isEqualTo(3);
        assertThat(criteria.experienceInclusive()).isFalse();
        assertThat(criteria.minimumGraduationCgpa()).isEqualTo(7.5);
        assertThat(criteria.minimumGraduationPercentage()).isEqualTo(75.0);
        assertThat(criteria.educationScoreOperator()).isEqualTo(EducationScoreOperator.ANY);
    }
}
