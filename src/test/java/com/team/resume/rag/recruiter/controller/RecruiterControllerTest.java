package com.team.resume.rag.recruiter.controller;

import com.team.resume.rag.recruiter.assistant.RecruiterAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecruiterController.class)
class RecruiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecruiterAssistant recruiterAssistant;

    @Test
    void createSessionReturnsSessionId() throws Exception {
        mockMvc.perform(post("/api/recruiter/session"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.welcomeMessage").isNotEmpty());
    }

    @Test
    void chatReturnsAssistantReply() throws Exception {
        when(recruiterAssistant.chat(eq("session-1"), eq("How many Java developers?")))
                .thenReturn("There are 2 Java developers.");

        mockMvc.perform(post("/api/recruiter/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "message": "How many Java developers?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("There are 2 Java developers."));
    }

    @Test
    void chatRejectsMissingMessage() throws Exception {
        mockMvc.perform(post("/api/recruiter/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
