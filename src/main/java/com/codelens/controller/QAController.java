package com.codelens.controller;

import com.codelens.dto.AskRequest;
import com.codelens.dto.AskResponse;
import com.codelens.entity.QAHistory;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.User;
import com.codelens.repository.QAHistoryRepository;
import com.codelens.repository.RepoAnalysisRepository;
import com.codelens.repository.UserRepository;
import com.codelens.service.RAGQueryService;
import com.codelens.service.RateLimitService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class QAController {

    private final RepoAnalysisRepository repoAnalysisRepository;
    private final QAHistoryRepository qaHistoryRepository;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;
    private final RAGQueryService ragQueryService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{repoId}/ask")
    public ResponseEntity<AskResponse> askQuestion(
            @PathVariable UUID repoId,
            @Valid @RequestBody AskRequest request) {

        User user = getAuthenticatedUser();
        RepoAnalysis repoAnalysis = repoAnalysisRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository analysis not found with ID: " + repoId));

        if (!repoAnalysis.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!rateLimitService.allowRequest(user.getId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. You have reached your quota of 20 questions per hour.");
        }

        log.info("Processing Q&A request for repo {} by user {}", repoId, user.getEmail());
        AskResponse response = ragQueryService.askQuestion(repoAnalysis, user, request.getQuestion());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{repoId}/history")
    public ResponseEntity<List<Map<String, Object>>> getQAHistory(@PathVariable UUID repoId) {
        User user = getAuthenticatedUser();
        RepoAnalysis repoAnalysis = repoAnalysisRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository analysis not found with ID: " + repoId));

        if (!repoAnalysis.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<QAHistory> histories = qaHistoryRepository.findByRepoAnalysisIdOrderByCreatedAtAsc(repoId);
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (QAHistory history : histories) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", history.getId());
            item.put("question", history.getQuestion());
            item.put("answer", history.getAnswer());
            item.put("createdAt", history.getCreatedAt());

            List<AskResponse.Citation> citations = new ArrayList<>();
            if (history.getCitations() != null && !history.getCitations().isBlank()) {
                try {
                    citations = objectMapper.readValue(history.getCitations(), new TypeReference<List<AskResponse.Citation>>() {});
                } catch (Exception e) {
                    log.warn("Could not deserialize QAHistory citations for {}: {}", history.getId(), e.getMessage());
                }
            }
            item.put("citations", citations);
            responseList.add(item);
        }

        return ResponseEntity.ok(responseList);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated user");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
