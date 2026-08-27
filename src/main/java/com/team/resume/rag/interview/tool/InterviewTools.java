package com.team.resume.rag.interview.tool;

import com.team.resume.rag.interview.domain.ScheduledInterview;
import com.team.resume.rag.interview.service.InterviewService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InterviewTools {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final InterviewService interviewService;

    public InterviewTools(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Tool("Schedule an interview for a candidate")
    public String scheduleInterview(
            @P("Candidate full name") String candidateName,
            @P("Candidate email address") String candidateEmail,
            @P("Candidate job profile, e.g. Java Developer") String profile,
            @P("Years of experience") int yearsOfExperience,
            @P("Interview date in yyyy-MM-dd format") String interviewDate,
            @P("Interview time in HH:mm format, 24-hour clock") String interviewTime
    ) {
        LocalDate date = parseDate(interviewDate);
        LocalTime time = parseTime(interviewTime);

        ScheduledInterview interview = interviewService.scheduleInterview(
                candidateName,
                candidateEmail,
                profile,
                yearsOfExperience,
                date,
                time
        );

        return "Interview scheduled for %s (%s) on %s at %s. Interview ID: %s".formatted(
                interview.getCandidateName(),
                interview.getProfile(),
                interview.getInterviewDate(),
                interview.getInterviewTime(),
                interview.getId()
        );
    }

    @Tool("Get scheduled interviews for a profile on a specific date")
    public String getScheduledInterviewsByProfileAndDate(
            @P("Job profile to filter by, e.g. Java Developer or Development") String profile,
            @P("Interview date in yyyy-MM-dd format") String interviewDate
    ) {
        LocalDate date = parseDate(interviewDate);
        List<ScheduledInterview> interviews = interviewService.findByProfileAndDate(profile, date);
        long count = interviewService.countByProfileAndDate(profile, date);

        if (interviews.isEmpty()) {
            return "No interviews scheduled for profile '%s' on %s.".formatted(profile, date);
        }

        String details = interviews.stream()
                .map(interview -> "- %s at %s (%s, %d years experience)".formatted(
                        interview.getCandidateName(),
                        interview.getInterviewTime(),
                        interview.getProfile(),
                        interview.getYearsOfExperience()
                ))
                .collect(Collectors.joining("\n"));

        return "%d interview(s) scheduled for profile '%s' on %s:\n%s".formatted(count, profile, date, details);
    }

    private LocalDate parseDate(String interviewDate) {
        try {
            return LocalDate.parse(interviewDate, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid interview date. Use yyyy-MM-dd format.");
        }
    }

    private LocalTime parseTime(String interviewTime) {
        try {
            return LocalTime.parse(interviewTime, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid interview time. Use HH:mm format.");
        }
    }
}
