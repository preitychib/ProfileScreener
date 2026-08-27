package com.team.resume.rag.resume.service;

import com.team.resume.rag.resume.dto.CandidateMatch;
import com.team.resume.rag.resume.dto.ResumeQueryResponse;
import com.team.resume.rag.resume.dto.RetrievedChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ResumeRagQueryService {

    private static final String NO_MATCH_ANSWER = "No matching resume excerpts were found in Redis.";

    private static final String ANSWER_PROMPT = """
            You are a recruiting assistant answering from retrieved resume excerpts only.
            If the excerpts do not contain the answer, say you do not know.
            The candidate field preceding each excerpt is trusted resume metadata.
            Use it to name candidates and deduplicate excerpts belonging to the same candidate.
            For questions requiring multiple skills, include a candidate only when the excerpts
            for that candidate provide evidence for every requested skill.

            Resume excerpts:
            %s

            Question: %s
            """;

    private final ContentRetriever contentRetriever;
    private final ChatModel chatModel;
    private final CandidateQueryParser candidateQueryParser;
    private final CandidateService candidateService;
    private final CandidateQueryMatcher candidateQueryMatcher;

    public ResumeRagQueryService(ContentRetriever contentRetriever,
                                 ChatModel chatModel,
                                 CandidateQueryParser candidateQueryParser,
                                 CandidateService candidateService,
                                 CandidateQueryMatcher candidateQueryMatcher) {
        this.contentRetriever = contentRetriever;
        this.chatModel = chatModel;
        this.candidateQueryParser = candidateQueryParser;
        this.candidateService = candidateService;
        this.candidateQueryMatcher = candidateQueryMatcher;
    }

    public ResumeQueryResponse query(String query) {
        CandidateQueryCriteria criteria = candidateQueryParser.parse(query);
        List<Content> contents = contentRetriever.retrieve(Query.from(query));
        List<RetrievedChunk> chunks = contents.stream()
                .map(this::toChunk)
                .toList();

        if (criteria.hasFilters()) {
            return structuredResponse(query, criteria, chunks);
        }

        if (chunks.isEmpty()) {
            return new ResumeQueryResponse(query, NO_MATCH_ANSWER, chunks, List.of());
        }

        String excerpts = IntStream.range(0, chunks.size())
                .mapToObj(index -> formattedExcerpt(index + 1, chunks.get(index)))
                .collect(Collectors.joining("\n\n"));
        String answer = chatModel.chat(ANSWER_PROMPT.formatted(excerpts, query));
        return new ResumeQueryResponse(query, answer, chunks, List.of());
    }

    private ResumeQueryResponse structuredResponse(String query,
                                                   CandidateQueryCriteria criteria,
                                                   List<RetrievedChunk> chunks) {
        List<CandidateMatch> matches = candidateQueryMatcher.match(candidateService.findAll(), criteria);
        if (matches.isEmpty()) {
            return new ResumeQueryResponse(
                    query,
                    "No candidates matched all requested criteria.",
                    List.of(),
                    matches
            );
        }

        Set<String> matchingCandidateIds = matches.stream()
                .map(match -> match.candidateId().toString())
                .collect(Collectors.toSet());
        List<RetrievedChunk> supportingChunks = chunks.stream()
                .filter(chunk -> chunk.candidateId() != null)
                .filter(chunk -> matchingCandidateIds.contains(chunk.candidateId()))
                .toList();

        String details = matches.stream()
                .map(this::formatMatch)
                .collect(Collectors.joining("\n"));
        String answer = "%d candidate(s) matched:%n%s".formatted(matches.size(), details);
        return new ResumeQueryResponse(query, answer, supportingChunks, matches);
    }

    private String formatMatch(CandidateMatch match) {
        return "- %s | profile: %s | matched: %s".formatted(
                match.name(),
                match.profile(),
                String.join("; ", match.matchedOn())
        );
    }

    private RetrievedChunk toChunk(Content content) {
        Metadata metadata = content.textSegment().metadata();
        return new RetrievedChunk(
                content.textSegment().text(),
                score(content),
                metadata.getString("fileName"),
                metadata.getString("candidateId"),
                metadata.getString("candidateName")
        );
    }

    private Double score(Content content) {
        Object value = content.metadata().get(ContentMetadata.SCORE);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private String formattedExcerpt(int index, RetrievedChunk chunk) {
        String source = chunk.fileName() == null ? "unknown" : chunk.fileName();
        String candidate = chunk.candidateName() == null ? "unknown" : chunk.candidateName();
        return "[%d] candidate=%s source=%s%n%s".formatted(index, candidate, source, chunk.text());
    }
}
