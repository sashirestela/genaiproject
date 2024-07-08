package com.encora.genai.data;

import static com.encora.genai.support.Commons.MAX_INDEX_LEVEL;
import static com.encora.genai.support.Commons.REFERENCE_SEPARATOR_FOR_SPLIT;

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
        String[] parts = reference.split(REFERENCE_SEPARATOR_FOR_SPLIT, 0);
        String lastPart = parts.length > MAX_INDEX_LEVEL
            ? parts[parts.length - 1]
            : reference;
        return lastPart;
    }

}
