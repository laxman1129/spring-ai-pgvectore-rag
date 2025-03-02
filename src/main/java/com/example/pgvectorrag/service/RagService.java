package com.example.pgvectorrag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
//@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final ChatModel chatModel; // without advisors
    private final ChatClient chatClient; // used for advisors
    private final VectorStore vectorStore;

    public RagService(ChatModel chatModel, ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatModel = chatModel;
        QuestionAnswerAdvisor questionAnswerAdvisor =
                new QuestionAnswerAdvisor(vectorStore,
                        SearchRequest.builder().similarityThreshold(0.8d).topK(6).build());

        ChatMemory chatMemory = new InMemoryChatMemory();
        PromptChatMemoryAdvisor memoryAdvisor = new PromptChatMemoryAdvisor(chatMemory);
        this.chatClient = chatClientBuilder
                .defaultAdvisors(memoryAdvisor, questionAnswerAdvisor)
                .build();
        this.vectorStore = vectorStore;
    }


    public String getResponse(String question) {
        log.info("Getting answer: {}", question);
        List<Document> documents = vectorStore
                .similaritySearch(SearchRequest.builder().query(question).topK(5).build());
        List<String> contentList = documents.stream().map(Document::getText).toList();

        PromptTemplate promptTemplate = new PromptTemplate("""
                You are an expert of Indian Constitution. Can you please help me with the following question?
                Question: {input}
                Here are some documents that might help you: {documents}
                """);
        Message userMessage = promptTemplate.createMessage(Map.of(
                "input", question,
                "documents", String.join("\n", contentList)
        ));

        log.info("userMessage: {}", userMessage.getText());
        log.info("""
                
                =================================================
                =================================================
                =================================================
                """);

        // calculate time taken to answer
        long startTime = System.currentTimeMillis();
        ChatResponse response = chatModel
                .call(new Prompt(List.of(userMessage)));

        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        // to seconds
        log.info("--------> Time taken to answer: {} seconds", timeTaken / 1000);

        String answer = response.getResult().getOutput().getText();
        log.info("answer: {}", answer);

        return answer;

    }

    public String getResponseUsingQAAdvisor(String question) {
        log.info("Getting answer: {}", question);
        QuestionAnswerAdvisor questionAnswerAdvisor =
                new QuestionAnswerAdvisor(vectorStore,
                        SearchRequest.builder().similarityThreshold(0.8d).topK(6).build());

        String content = chatClient
                .prompt()
                .advisors(questionAnswerAdvisor)
                .user(question)
                .call()
                .content();
        log.info("answer: {}", content);
        return content;

    }

    public String getResponseUsingRAGAdvisor(String question) {
        log.info("Getting answer using getResponseUsingRAGAdvisor : {}", question);
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        String content = chatClient
                .prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();
        log.info("answer: {}", content);
        return content;

    }

    public String getResponseUsingMemory(String question) {
        log.info("Getting answer using getResponseUsingRAGAdvisor : {}", question);
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();
        String content = prompt
                .user(question)
                .call()
                .content();
        log.info("answer: {}", content);
        return content;

    }
}
