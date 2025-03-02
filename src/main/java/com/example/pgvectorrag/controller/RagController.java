package com.example.pgvectorrag.controller;

import com.example.pgvectorrag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagService ragService;

    @GetMapping("/ask")
    public String ask(String question) {
        log.info("Asking question: {}", question);
        return ragService.getResponse(question);
    }

    @GetMapping("/ask/qaadvisor")
    public String askQAAdvisor(String question) {
        log.info("Asking question: {}", question);
        return ragService.getResponseUsingQAAdvisor(question);
    }

    @GetMapping("/ask/ragadvisor")
    public String askRagAdvisor(String question) {
        log.info("Asking question askRagAdvisor : {}", question);
        return ragService.getResponseUsingRAGAdvisor(question);
    }

    @GetMapping("/ask/memory")
    public String askMemory(String question) {
        log.info("Asking question askRagAdvisor : {}", question);
        return ragService.getResponseUsingMemory(question);
    }
}
