package com.team.resume.rag.candidate.tool;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.service.CandidateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateToolsTest {

    @Mock
    private CandidateService candidateService;

    @InjectMocks
    private CandidateTools candidateTools;

    @Test
    void countCandidatesByProfileReturnsFormattedCount() {
        when(candidateService.countByProfile("Development")).thenReturn(3L);

        String response = candidateTools.countCandidatesByProfile("Development");

        assertThat(response).contains("3 candidate(s)").contains("Development");
    }

    @Test
    void findCandidatesWithTechnologyReturnsCandidateDetails() {
        Candidate candidate = new Candidate(
                UUID.randomUUID(),
                "Rahul Sharma",
                "rahul@example.com",
                "Java Developer",
                5,
                "Java, Spring Boot, Redis",
                "rahul-java-5.pdf"
        );
        when(candidateService.findByTechnology("Spring Boot")).thenReturn(List.of(candidate));

        String response = candidateTools.findCandidatesWithTechnology("Spring Boot");

        assertThat(response).contains("Rahul Sharma").contains("Spring Boot");
    }
}
