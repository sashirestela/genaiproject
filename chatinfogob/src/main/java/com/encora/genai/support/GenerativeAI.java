package com.encora.genai.support;

import static com.encora.genai.support.Commons.COMPLETION_MODEL;
import static com.encora.genai.support.Commons.COMPLETION_TEMPERATURE;
import static com.encora.genai.support.Commons.EMBEDDING_DIMENSIONS;
import static com.encora.genai.support.Commons.EMBEDDING_MODEL;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.tool.Tool;
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

    public static Chat executeChatCompletion(List<ChatMessage> messages, List<Tool> tools) {
        ChatRequest request = ChatRequest.builder()
                .model(COMPLETION_MODEL)
                .temperature(COMPLETION_TEMPERATURE)
                .messages(messages)
                .tools(tools)
                .build();
        Chat response = OPENAI.chatCompletions().create(request).join();
        LOGGER.debug("Chat Completion was executed.");
        return response;
    }

}
