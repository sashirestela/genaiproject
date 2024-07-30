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

    public static final String MIMETYPE_PDF = PropertyLoader.getString("app.mimetype_pdf");
    public static final String UPLOAD_FOLDER = PropertyLoader.getString("app.upload_folder");

    public static final Integer MAX_INDEX_LEVEL = PropertyLoader.getInteger("splitter.max_index_level");
    public static final String LEVEL_JOINNER = PropertyLoader.getString("splitter.level_joinner");
    public static final String FIELD_SEPARATOR = PropertyLoader.getString("splitter.field_separator");
    public static final String FIELD_SEPARATOR_REGEX = PropertyLoader.getString("splitter.field_separator_regex");
    public static final Integer MAX_NUM_CHARS = PropertyLoader.getInteger("splitter.max_num_chars");
    public static final String TO_CLEAN_REGEX = PropertyLoader.getString("splitter.to_clean_regex");
    public static final String[] BY_LEVEL_REGEX = PropertyLoader.getArray("splitter.by_level_regex");

    public static final String START_TEXT_FOR_LAST_PART = PropertyLoader.getString("fragment.start_text_for_last_part");
    public static final String CONTENT_SEPARATOR = PropertyLoader.getString("fragment.content_separator");

    public static final String START_MARKER = PropertyLoader.getString("quoute.start_marker");
    public static final String COL_SEPARATOR = PropertyLoader.getString("quote.col_separator");
    public static final String ROW_SEPARATOR = PropertyLoader.getString("quote.row_separator");

    public static final Integer BATCH_SIZE = PropertyLoader.getInteger("db.batch_size");

    public static final String EMBEDDING_MODEL = PropertyLoader.getString("ai.embedding.model");
    public static final Integer EMBEDDING_DIMENSIONS = PropertyLoader.getInteger("ai.embedding.dimensions");
    public static final String COMPLETION_MODEL = PropertyLoader.getString("ai.completion.model");
    public static final Double COMPLETION_TEMPERATURE = PropertyLoader.getDouble("ai.completion.temperature");

    public static final Double MATCH_THRESHOLD = PropertyLoader.getDouble("search.match_threshold");
    public static final Integer MATCH_COUNT = PropertyLoader.getInteger("search.match_count");

    public static final String SAMPLE_CONTEXT = PropertyLoader.getString("sample.context");
    public static final String SAMPLE_QUESTION = PropertyLoader.getString("sample.question");

    public static final String SAMPLE_ENHANCED_QUESTION = PropertyLoader.addSpacer(
            PropertyLoader.getString("template.enhanced_question",
                    Map.of("contextForQuestion", SAMPLE_CONTEXT, "rewrittenQuestion", SAMPLE_QUESTION)));
    public static final String SAMPLE_ANSWER = PropertyLoader.addSpacer(PropertyLoader.getString("sample.answer"));

    public static final String PROMPT_MAIN_STEP_SYSTEM = PropertyLoader.getString("prompt.main_step.system",
            Map.of("sampleEnhancedQuestion", SAMPLE_ENHANCED_QUESTION, "sampleAnswer", SAMPLE_ANSWER));
    public static final String PROMPT_PREV_STEP_SYSTEM = PropertyLoader.getString("prompt.prev_step.system");
    public static final String PROMPT_NO_CONTEXT = PropertyLoader.getString("prompt.no_context");

    public static final String TEMPLATE_ENHANCED_QUESTION = PropertyLoader.getString("template.enhanced_question");
    public static final String TEMPLATE_CONTEXT_FRAGMENT = PropertyLoader.getString("template.context_fragment");

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
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

}
