package com.team.resume.rag.resume.controller;

import com.team.resume.rag.resume.dto.ResumeQueryResponse;
import com.team.resume.rag.resume.dto.RetrievedChunk;
import com.team.resume.rag.resume.service.ResumeIngestionService;
import com.team.resume.rag.resume.service.ResumeRagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeIngestionService resumeIngestionService;

    @MockBean
    private ResumeRagQueryService resumeRagQueryService;

    @Test
    void ingestReturnsIngestedFiles() throws Exception {
        when(resumeIngestionService.ingestAllFromDirectory())
                .thenReturn(new ResumeIngestionService.IngestResult(
                        List.of("john-java-5.pdf"),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(post("/api/resumes/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingested[0]").value("john-java-5.pdf"));
    }

    @Test
    void uploadRejectsNonPdfFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "plain text".getBytes()
        );

        when(resumeIngestionService.ingestUploadedFile(any()))
                .thenThrow(new IllegalArgumentException("Only PDF files are supported"));

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCandidatesReturnsEmptyArray() throws Exception {
        when(resumeIngestionService.listCandidates()).thenReturn(List.of());

        mockMvc.perform(get("/api/resumes/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void queryReturnsAnswerAndRetrievedChunks() throws Exception {
        when(resumeRagQueryService.query(eq("Who has Spring Boot experience?")))
                .thenReturn(new ResumeQueryResponse(
                        "Who has Spring Boot experience?",
                        "Rahul Sharma has Spring Boot experience.",
                        List.of(new RetrievedChunk(
                                "Rahul Sharma has 5 years of Spring Boot experience.",
                                0.91,
                                "rahul-java-5.pdf",
                                "candidate-1",
                                "Rahul Sharma"
                        )),
                        List.of()
                ));

        mockMvc.perform(post("/api/resumes/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "Who has Spring Boot experience?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Rahul Sharma has Spring Boot experience."))
                .andExpect(jsonPath("$.retrievedChunks[0].fileName").value("rahul-java-5.pdf"))
                .andExpect(jsonPath("$.retrievedChunks[0].candidateName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.retrievedChunks[0].score").value(0.91));
    }

    @Test
    void queryRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/resumes/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
