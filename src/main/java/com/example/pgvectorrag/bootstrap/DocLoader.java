package com.example.pgvectorrag.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocLoader implements CommandLineRunner {
    private final VectorStore vectorStore;

    @Value("${application.aiapp.docToLoadDb}")
    private Resource doc;

    @Override
    public void run(String... args) {
        log.info("Loading vector store");
        try {
            if (Objects.requireNonNull(vectorStore.similaritySearch("doc")).isEmpty()) {
                log.info("No doc found");
                loadStore();
            }
        } catch (Exception e) {
            log.error("Error loading vector store", e);
        }

        log.info("Vectorstore loaded");
    }


    private void loadStore() {
        TikaDocumentReader reader = new TikaDocumentReader(doc);
        List<Document> documents = reader.read();
        TextSplitter textSplitter =
                new TokenTextSplitter(100, 50, 5, 10000, true);
        List<Document> splitDocs = textSplitter.apply(documents);
        vectorStore.add(splitDocs);
    }
}
