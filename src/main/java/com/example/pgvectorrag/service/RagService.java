package com.example.pgvectorrag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;


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

        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .advisors(questionAnswerAdvisor)
                .user(question)
                .call()
                .content();

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

        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();

    }

    public String getResponseUsingMemory(String question) {
        log.info("Getting answer using getResponseUsingRAGAdvisor : {}", question);
        QuestionAnswerAdvisor questionAnswerAdvisor =
                new QuestionAnswerAdvisor(vectorStore,
                        SearchRequest.builder().similarityThreshold(0.8d).topK(6).build());

        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);

        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .advisors(messageChatMemoryAdvisor, questionAnswerAdvisor)
                .user(question)
                .call()
                .content();

    }
}
