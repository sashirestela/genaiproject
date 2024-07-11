package com.encora.genai.data;

import static com.encora.genai.support.Commons.START_TEXT_FOR_LAST_PART;
import static com.encora.genai.support.Commons.FIELD_SEPARATOR_REGEX;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FragmentResult {

    private String reference;
    private String content;
    private Double similarity;
    private Integer rowid;

    public String lastPartReference() {
        String[] parts = reference.split(FIELD_SEPARATOR_REGEX, 0);
        String candidateLastPart = parts[parts.length - 1];
        String lastPart = candidateLastPart.startsWith(START_TEXT_FOR_LAST_PART) ? candidateLastPart : reference;
        return lastPart;
    }

}
