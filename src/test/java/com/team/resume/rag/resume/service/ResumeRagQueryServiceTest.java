package com.team.resume.rag.resume.service;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.dto.CandidateMatch;
import com.team.resume.rag.resume.dto.ResumeQueryResponse;
import com.team.resume.rag.resume.dto.RetrievedChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeRagQueryServiceTest {

    @Mock
    private ContentRetriever contentRetriever;

    @Mock
    private ChatModel chatModel;

    @Mock
    private CandidateQueryParser candidateQueryParser;

    @Mock
    private CandidateService candidateService;

    private ResumeRagQueryService resumeRagQueryService;

    @BeforeEach
    void setUp() {
        resumeRagQueryService = new ResumeRagQueryService(
                contentRetriever,
                chatModel,
                candidateQueryParser,
                candidateService,
                new CandidateQueryMatcher()
        );
        when(candidateQueryParser.parse(anyString())).thenReturn(emptyCriteria());
    }

    @Test
    void answersFromRetrievedRedisChunks() {
        Content content = Content.from(
                TextSegment.from(
                        "Rahul Sharma has 5 years of Spring Boot experience.",
                        Metadata.from(Map.of(
                                "fileName", "rahul-java-5.pdf",
                                "candidateId", "candidate-1",
                                "candidateName", "Rahul Sharma"
                        ))
                ),
                Map.of(ContentMetadata.SCORE, 0.91)
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));
        when(chatModel.chat(any(String.class))).thenReturn("Rahul Sharma has Spring Boot experience.");

        ResumeQueryResponse response = resumeRagQueryService.query("Who has Spring Boot experience?");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(contentRetriever).retrieve(queryCaptor.capture());
        assertThat(queryCaptor.getValue().text()).isEqualTo("Who has Spring Boot experience?");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("candidate=Rahul Sharma");
        assertThat(promptCaptor.getValue()).contains("Rahul Sharma has 5 years of Spring Boot experience.");
        assertThat(promptCaptor.getValue()).contains("Who has Spring Boot experience?");

        assertThat(response.query()).isEqualTo("Who has Spring Boot experience?");
        assertThat(response.answer()).isEqualTo("Rahul Sharma has Spring Boot experience.");
        assertThat(response.retrievedChunks()).containsExactly(
                new RetrievedChunk(
                        "Rahul Sharma has 5 years of Spring Boot experience.",
                        0.91,
                        "rahul-java-5.pdf",
                        "candidate-1",
                        "Rahul Sharma"
                )
        );
    }

    @Test
    void skipsGenerationWhenNoChunksMatch() {
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of());

        ResumeQueryResponse response = resumeRagQueryService.query("Who knows COBOL?");

        verify(chatModel, never()).chat(any(String.class));
        assertThat(response.answer()).isEqualTo("No matching resume excerpts were found in Redis.");
        assertThat(response.retrievedChunks()).isEmpty();
    }

    @Test
    void deterministicallyMatchesStructuredCandidateFilters() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = new Candidate(
                candidateId,
                "Aakanksha",
                "aakanksha@example.com",
                "Java Developer",
                4,
                "Java, Spring Boot, AWS, Kafka",
                8.1,
                null,
                "aakanksha.pdf"
        );
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
        when(candidateQueryParser.parse(anyString())).thenReturn(criteria);
        when(candidateService.findAll()).thenReturn(List.of(candidate));
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of());

        ResumeQueryResponse response =
                resumeRagQueryService.query("Name candidates having experience in AWS and Spring Boot");

        verify(chatModel, never()).chat(anyString());
        assertThat(response.answer())
                .contains("Aakanksha")
                .contains("AWS")
                .contains("Spring Boot");
        assertThat(response.matchedCandidates()).extracting(CandidateMatch::name)
                .containsExactly("Aakanksha");
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
