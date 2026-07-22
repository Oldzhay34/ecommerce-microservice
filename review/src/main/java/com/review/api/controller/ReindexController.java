package com.review.api.controller;

import com.review.infrastructure.persistence.mapper.ReviewEntityMapper;
import com.review.infrastructure.persistence.repository.ReviewRepository;
import com.review.infrastructure.search.adapter.ReviewSearchAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class ReindexController {

    private final ReviewRepository reviewRepository;
    private final ReviewEntityMapper entityMapper;
    private final ReviewSearchAdapter searchAdapter;

    public ReindexController(ReviewRepository reviewRepository,
                             ReviewEntityMapper entityMapper,
                             ReviewSearchAdapter searchAdapter) {
        this.reviewRepository = reviewRepository;
        this.entityMapper = entityMapper;
        this.searchAdapter = searchAdapter;
    }

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        var all = reviewRepository.findAll();
        all.forEach(e -> searchAdapter.syncToElasticsearch(entityMapper.toDomain(e)));
        return ResponseEntity.ok(Map.of("indexed", all.size()));
    }
}