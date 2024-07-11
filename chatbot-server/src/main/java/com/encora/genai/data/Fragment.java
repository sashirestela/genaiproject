package com.encora.genai.data;

import static com.encora.genai.support.Commons.CONTENT_SEPARATOR;
import static com.encora.genai.support.Commons.FIELD_SEPARATOR_REGEX;
import static com.encora.genai.support.Commons.START_TEXT_FOR_LAST_PART;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Fragment {

    private Long id;
    private String reference;
    private String content;
    private List<Double> embedding;

    public void updateContentWithReference() {
        content = lastPartReference() + CONTENT_SEPARATOR + content;
    }

    private String lastPartReference() {
        String[] parts = reference.split(FIELD_SEPARATOR_REGEX, 0);
        String candidateLastPart = parts[parts.length - 1];
        String lastPart = candidateLastPart.startsWith(START_TEXT_FOR_LAST_PART) ? candidateLastPart : reference;
        return lastPart;
    }

}
