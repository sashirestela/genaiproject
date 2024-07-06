package com.encora.genai.support;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.embedding.Embedding;
import io.github.sashirestela.openai.domain.embedding.EmbeddingFloat;
import io.github.sashirestela.openai.domain.embedding.EmbeddingRequest;
import io.github.sashirestela.openai.domain.embedding.EmbeddingRequest.EncodingFormat;

public class GenerativeAI {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerativeAI.class);
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final Integer EMBEDDING_DIMENSIONS = 1536;
    private static final String API_KEY;

    static {
        API_KEY = System.getenv("OPENAI_API_KEY");
    }

    private GenerativeAI() {
    }

    public static List<List<Double>> createEmbeddings(List<String> contents) {
        SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(API_KEY).build();
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(contents)
                .model(EMBEDDING_MODEL)
                .encodingFormat(EncodingFormat.FLOAT)
                .dimensions(EMBEDDING_DIMENSIONS)
                .build();
        Embedding<EmbeddingFloat> response = openAI.embeddings().create(request).join();
        LOGGER.debug("Embeddings were created for {} contents.", contents.size());
        return response.getData().stream().map(emb -> emb.getEmbedding()).toList();
    }

    public static List<Double> createEmbedding(String content) {
        return createEmbeddings(Arrays.asList(content)).get(0);
    }

}
