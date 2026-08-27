package com.team.resume.rag.interview.service;

import com.team.resume.rag.interview.domain.ScheduledInterview;
import com.team.resume.rag.interview.repository.InterviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(InterviewService.class)
class InterviewServiceTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewRepository interviewRepository;

    @Test
    void schedulesAndFindsInterviewsByProfileAndDate() {
        interviewService.scheduleInterview(
                "Jane Doe",
                "jane@example.com",
                "Java Developer",
                6,
                LocalDate.of(2026, 8, 25),
                LocalTime.of(10, 0)
        );
        interviewService.scheduleInterview(
                "John Smith",
                "john@example.com",
                "Java Developer",
                4,
                LocalDate.of(2026, 8, 25),
                LocalTime.of(14, 30)
        );
        interviewService.scheduleInterview(
                "Alice QA",
                "alice@example.com",
                "QA Engineer",
                3,
                LocalDate.of(2026, 8, 25),
                LocalTime.of(11, 0)
        );

        List<ScheduledInterview> javaInterviews =
                interviewService.findByProfileAndDate("Java Developer", LocalDate.of(2026, 8, 25));

        assertThat(javaInterviews).hasSize(2);
        assertThat(interviewService.countByProfileAndDate("Java Developer", LocalDate.of(2026, 8, 25))).isEqualTo(2);
        assertThat(interviewRepository.count()).isEqualTo(3);
    }
}
