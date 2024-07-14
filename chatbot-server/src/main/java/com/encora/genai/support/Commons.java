package com.encora.genai.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import com.encora.genai.util.PropertyLoader;

public class Commons {

    private Commons() {
    }

    public static final Integer HTTP_PORT = PropertyLoader.getInteger("app.http.port");
    public static final Integer THREAD_POOL_SIZE = PropertyLoader.getInteger("app.thread.pool.size");

    public static final Integer MAX_INDEX_LEVEL = PropertyLoader.getInteger("splitter.max_index_level");
    public static final String LEVEL_JOINNER = PropertyLoader.getString("splitter.level.joinner");
    public static final String FIELD_SEPARATOR = PropertyLoader.getString("splitter.field.separator");
    public static final String FIELD_SEPARATOR_REGEX = PropertyLoader.getString("splitter.field.separator.regex");
    public static final Integer MAX_NUM_CHARS = PropertyLoader.getInteger("splitter.max_num_chars");
    public static final String TO_CLEAN_REGEX = PropertyLoader.getString("splitter.to_clean.regex");
    public static final String[] BY_LEVEL_REGEX = PropertyLoader.getArray("splitter.by_level.regex");

    public static final String START_TEXT_FOR_LAST_PART = PropertyLoader.getString("fragment.start_text_for_last_part");
    public static final String CONTENT_SEPARATOR = PropertyLoader.getString("fragment.content_separator");

    public static final String START_MARKER = PropertyLoader.getString("quoute.start.marker");
    public static final String COL_SEPARATOR = PropertyLoader.getString("quote.col.separator");
    public static final String ROW_SEPARATOR = PropertyLoader.getString("quote.row.separator");

    public static final Integer BATCH_SIZE = PropertyLoader.getInteger("db.batch.size");

    public static final String EMBEDDING_MODEL = PropertyLoader.getString("ai.embedding.model");
    public static final Integer EMBEDDING_DIMENSIONS = PropertyLoader.getInteger("ai.embedding.dimensions");
    public static final String COMPLETION_MODEL = PropertyLoader.getString("ai.completion.model");
    public static final Double COMPLETION_TEMPERATURE = PropertyLoader.getDouble("ai.completion.temperature");

    public static final Double MATCH_THRESHOLD = PropertyLoader.getDouble("search.match.threshold");
    public static final Integer MATCH_COUNT = PropertyLoader.getInteger("search.match.count");

    public static final String SAMPLE_CONTEXT = PropertyLoader.getString("prompt.sample.context");
    public static final String SAMPLE_QUESTION = PropertyLoader.getString("prompt.sample.question");
    public static final String SAMPLE_ANSWER = PropertyLoader.addSpacer(
            PropertyLoader.getString("prompt.sample.answer"));
    public static final String SAMPLE_ENHANCED_QUESTION = PropertyLoader.addSpacer(
            PropertyLoader.getString("prompt.enhanced.question",
                    Map.of("contextForQuestion", SAMPLE_CONTEXT, "rewrittenQuestion", SAMPLE_QUESTION)));

    public static final String PROMPT_SYSTEM = PropertyLoader.getString("prompt.system",
            Map.of("sampleEnhancedQuestion", SAMPLE_ENHANCED_QUESTION, "sampleAnswer", SAMPLE_ANSWER));
    public static final String PROMPT_REWRITE_QUESTION = PropertyLoader.getString("prompt.rewrite.question");
    public static final String PROMPT_ENHANCED_QUESTION = PropertyLoader.getString("prompt.enhanced.question");
    public static final String PROMPT_WITHOUT_INFORMATION = PropertyLoader.getString("prompt.without.information");

    public static final String EMPTY = "";

    public static String getUserInput(String message, String defaultValue) {
        String input = System.console().readLine(message);
        if (input == null || input.isBlank()) {
            input = defaultValue;
        }
        return input;
    }

    public static String replacePlaceholders(String value, Map<String, String> replacement) {
        String result = value;
        for (Entry<String, String> entry : replacement.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder , entry.getValue());
        }
        return result;
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

}
