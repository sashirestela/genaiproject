package com.encora.genai.support;

import static com.encora.genai.support.Commons.COL_SEPARATOR;
import static com.encora.genai.support.Commons.ROW_SEPARATOR;
import static com.encora.genai.support.Commons.START_MARKER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.FragmentResult;

public class Quote {

    private static final Logger LOGGER = LoggerFactory.getLogger(Quote.class);

    private Quote() {
    }

    public static boolean isQuote(String probableQuote) {
        return probableQuote.contains(START_MARKER);
    }

    public static String serializeForQuotes(List<FragmentResult> fragments) {
        String serialized = START_MARKER + fragments.stream()
                .map(fr -> fr.getRowid() + COL_SEPARATOR + fr.getContentHeader())
                .collect(Collectors.joining(ROW_SEPARATOR));
        LOGGER.debug("Serialized text: {}", serialized);
        return serialized;
    }

    public static Map<String, String> matchQuotes(String text, String serializedQuote) {
        Map<String, String> baseQuotes = deserialize(serializedQuote);
        Map<String, String> extracted = baseQuotes.entrySet().stream()
                .filter(bq -> text.contains("[" + bq.getKey() + "]"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return extracted;
    }

    private static Map<String, String> deserialize(String serializedQuote) {
        Map<String, String> map = new HashMap<>();
        String cleanedSerializedQuote = serializedQuote.substring(START_MARKER.length());
        StringTokenizer tokenizer = new StringTokenizer(cleanedSerializedQuote, ROW_SEPARATOR);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String[] keyValue = token.split(COL_SEPARATOR);
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }

}
