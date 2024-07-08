package com.encora.genai.support;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.embedding.Embedding;
import io.github.sashirestela.openai.domain.embedding.EmbeddingFloat;
import io.github.sashirestela.openai.domain.embedding.EmbeddingRequest;
import io.github.sashirestela.openai.domain.embedding.EmbeddingRequest.EncodingFormat;

public class GenerativeAI {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerativeAI.class);

    private static final String OPENAI_API_KEY;
    private static final SimpleOpenAI OPENAI;

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final Integer EMBEDDING_DIMENSIONS = 1536;

    private static final String COMPLETION_MODEL = "gpt-4o";
    //private static final String COMPLETION_MODEL = "gpt-3.5-turbo";
    private static final Double COMPLETION_TEMPERATURE = 0.2;

    static {
        OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
        OPENAI = SimpleOpenAI.builder().apiKey(OPENAI_API_KEY).build();
    }

    private GenerativeAI() {
    }

    public static List<List<Double>> createEmbeddings(List<String> contents) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(contents)
                .model(EMBEDDING_MODEL)
                .encodingFormat(EncodingFormat.FLOAT)
                .dimensions(EMBEDDING_DIMENSIONS)
                .build();
        Embedding<EmbeddingFloat> response = OPENAI.embeddings().create(request).join();
        LOGGER.debug("Embeddings were created for {} contents.", contents.size());
        return response.getData().stream().map(emb -> emb.getEmbedding()).toList();
    }

    public static List<Double> createEmbedding(String content) {
        return createEmbeddings(Arrays.asList(content)).get(0);
    }

    public static Stream<Chat> executeSreamChatCompletion(List<ChatMessage> messages) {
        ChatRequest request = ChatRequest.builder()
                .model(COMPLETION_MODEL)
                .temperature(COMPLETION_TEMPERATURE)
                .messages(messages)
                .build();
        Stream<Chat> response = OPENAI.chatCompletions().createStream(request).join();
        LOGGER.debug("Stream Chat Completion was executed.");
        return response;
    }

    public static Chat executeChatCompletion(List<ChatMessage> messages) {
        ChatRequest request = ChatRequest.builder()
                .model(COMPLETION_MODEL)
                .temperature(COMPLETION_TEMPERATURE)
                .messages(messages)
                .build();
        Chat response = OPENAI.chatCompletions().create(request).join();
        LOGGER.debug("Chat Completion was executed.");
        return response;
    }

}
