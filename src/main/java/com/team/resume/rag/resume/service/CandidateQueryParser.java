package com.team.resume.rag.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateQueryParser {

    private static final Logger log = LoggerFactory.getLogger(CandidateQueryParser.class);

    private static final String CRITERIA_PROMPT = """
            Extract candidate filters from the user query.
            Return ONLY valid JSON using this exact structure:
            {
              "requiredTechnologies": ["technology names that must all match"],
              "minimumYearsExperience": null,
              "experienceInclusive": false,
              "minimumGraduationCgpa": null,
              "cgpaInclusive": false,
              "minimumGraduationPercentage": null,
              "percentageInclusive": false,
              "educationScoreOperator": "ANY"
            }

            Rules:
            - Extract only filters explicitly requested by the user.
            - "more than" and "above" are exclusive; "at least" is inclusive.
            - Education scores refer only to the graduation/bachelor's degree.
            - Use educationScoreOperator "ANY" for OR and "ALL" for AND.
            - Keep absent numeric filters as null and absent technologies as [].

            User query:
            %s
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public CandidateQueryParser(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public CandidateQueryCriteria parse(String query) {
        try {
            String response = chatModel.chat(CRITERIA_PROMPT.formatted(query));
            return objectMapper.readValue(extractJson(response), CandidateQueryCriteria.class);
        } catch (Exception exception) {
            log.warn("Could not parse structured candidate filters; using semantic RAG only", exception);
            return emptyCriteria();
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object found in query parser response");
        }
        return response.substring(start, end + 1);
    }

    private CandidateQueryCriteria emptyCriteria() {
        return new CandidateQueryCriteria(
                List.of(),
                null,
                false,
                null,
                false,
                null,
                false,
                EducationScoreOperator.ANY
        );
    }
}
