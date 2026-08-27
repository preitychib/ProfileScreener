package com.team.resume.rag.recruiter.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface RecruiterAssistant {

    @SystemMessage("""
            You are a recruiting assistant for a hiring team.
            Use retrieved resume context to answer questions about candidate skills, experience, and suitability.
            Use tools for:
            - counting candidates by profile or technology
            - listing all candidates
            - scheduling interviews
            - checking scheduled interviews by profile and date
            When recommending candidates, always mention their names.
            When scheduling interviews, confirm candidate details before calling the schedule tool.
            For date-based interview queries, use yyyy-MM-dd format.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
