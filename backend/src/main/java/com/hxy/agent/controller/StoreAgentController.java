package com.hxy.agent.controller;

import com.hxy.agent.model.AnalysisRequest;
import com.hxy.agent.model.AnalysisResponse;
import com.hxy.agent.model.AnalysisResult;
import com.hxy.agent.model.FollowUpRequest;
import com.hxy.agent.service.StoreAnalysisAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class StoreAgentController {

    private final StoreAnalysisAgent agent;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestBody AnalysisRequest request,
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId
    ) {
        log.info("分析请求: query={}, sessionId={}", request.getQuery(), sessionId);

        AnalysisResult result = agent.analyze(sessionId, request.getQuery());

        return ResponseEntity.ok(AnalysisResponse.from(result));
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> analyzeStream(
            @RequestBody AnalysisRequest request,
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId
    ) {
        log.info("流式分析请求: query={}, sessionId={}", request.getQuery(), sessionId);

        return agent.analyzeStream(sessionId, request.getQuery());
    }

    @PostMapping("/follow-up")
    public ResponseEntity<AnalysisResponse> followUp(
            @RequestBody FollowUpRequest request,
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId
    ) {
        log.info("追问请求: question={}, sessionId={}", request.getQuestion(), sessionId);

        AnalysisResult result = agent.analyze(sessionId, request.getQuestion());

        return ResponseEntity.ok(AnalysisResponse.from(result));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}