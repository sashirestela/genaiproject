package com.encora.genai.data;

import static com.encora.genai.support.Commons.CONTENT_SEPARATOR;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FragmentResult {

    private String reference;
    private String content;
    private Double similarity;
    private Integer rowid;

    public String getContentHeader() {
        return content.split(CONTENT_SEPARATOR, 0)[0];
    }
}
