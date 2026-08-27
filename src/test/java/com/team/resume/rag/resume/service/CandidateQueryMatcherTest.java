package com.team.resume.rag.resume.service;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.dto.CandidateMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateQueryMatcherTest {

    private final CandidateQueryMatcher matcher = new CandidateQueryMatcher();

    @Test
    void matchesAllRequiredTechnologies() {
        Candidate matching = candidate("Aakanksha", 4, "Java, Spring Boot, AWS, Kafka", 8.1, null);
        Candidate missingAws = candidate("Rahul", 5, "Java, Spring Boot, Kafka", 8.5, null);
        CandidateQueryCriteria criteria = new CandidateQueryCriteria(
                List.of("AWS", "Spring Boot"),
                null,
                false,
                null,
                false,
                null,
                false,
                EducationScoreOperator.ANY
        );

        List<CandidateMatch> matches = matcher.match(List.of(matching, missingAws), criteria);

        assertThat(matches).extracting(CandidateMatch::name).containsExactly("Aakanksha");
        assertThat(matches.get(0).matchedOn())
                .contains("technologies: AWS, Spring Boot");
    }

    @Test
    void matchesExperienceAndEitherGraduationScore() {
        Candidate cgpaMatch = candidate("Aakanksha", 4, "Java", 8.1, null);
        Candidate percentageMatch = candidate("Rahul", 5, "Java", null, 78.0);
        Candidate insufficientExperience = candidate("Neha", 3, "Java", 9.0, null);
        Candidate insufficientEducation = candidate("Amit", 6, "Java", 7.2, 70.0);
        CandidateQueryCriteria criteria = new CandidateQueryCriteria(
                List.of(),
                3,
                false,
                7.5,
                false,
                75.0,
                false,
                EducationScoreOperator.ANY
        );

        List<CandidateMatch> matches = matcher.match(
                List.of(cgpaMatch, percentageMatch, insufficientExperience, insufficientEducation),
                criteria
        );

        assertThat(matches).extracting(CandidateMatch::name)
                .containsExactly("Aakanksha", "Rahul");
    }

    private Candidate candidate(String name,
                                int yearsOfExperience,
                                String technologies,
                                Double graduationCgpa,
                                Double graduationPercentage) {
        return new Candidate(
                UUID.randomUUID(),
                name,
                name.toLowerCase() + "@example.com",
                "Java Developer",
                yearsOfExperience,
                technologies,
                graduationCgpa,
                graduationPercentage,
                name.toLowerCase() + ".pdf"
        );
    }
}
