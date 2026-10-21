package com.findex.controller;

import com.findex.core.model.BenchmarkResult;
import com.findex.service.BenchmarkService;
import com.findex.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BenchmarkController {
    private final BenchmarkService benchmarkService;
    private final EvaluationService evaluationService;

    public BenchmarkController(BenchmarkService benchmarkService, EvaluationService evaluationService) {
        this.benchmarkService = benchmarkService;
        this.evaluationService = evaluationService;
    }

    @PostMapping("/benchmark")
    public ResponseEntity<BenchmarkResult> runBenchmark(@RequestParam(name = "q", defaultValue = "java") String query) {
        return ResponseEntity.ok(benchmarkService.runBenchmark(query));
    }

    @GetMapping("/evaluation")
    public ResponseEntity<Map<String, Object>> evaluate(
            @RequestParam(name = "mode", defaultValue = "bm25") String mode,
            @RequestParam(name = "k", defaultValue = "5") int k) {
        return ResponseEntity.ok(evaluationService.evaluate(mode, k));
    }
}
