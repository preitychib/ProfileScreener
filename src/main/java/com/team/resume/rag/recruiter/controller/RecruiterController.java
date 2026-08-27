package com.team.resume.rag.recruiter.controller;

import com.team.resume.rag.recruiter.assistant.RecruiterAssistant;
import com.team.resume.rag.recruiter.dto.RecruiterChatRequest;
import com.team.resume.rag.recruiter.dto.RecruiterChatResponse;
import com.team.resume.rag.recruiter.dto.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private static final String WELCOME_MESSAGE =
            "Ask about candidate profiles, technologies, schedule interviews, or check scheduled interviews.";

    private final RecruiterAssistant recruiterAssistant;

    public RecruiterController(RecruiterAssistant recruiterAssistant) {
        this.recruiterAssistant = recruiterAssistant;
    }

    @PostMapping("/session")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession() {
        String sessionId = UUID.randomUUID().toString();
        return new SessionResponse(sessionId, WELCOME_MESSAGE);
    }

    @PostMapping("/chat")
    public RecruiterChatResponse chat(@RequestBody RecruiterChatRequest request) {
        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }

        String reply = recruiterAssistant.chat(request.sessionId(), request.message());
        return new RecruiterChatResponse(request.sessionId(), reply);
    }
}
