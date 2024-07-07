package com.encora.genai.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FragmentResult {

    private String reference;
    private String content;
    private Double similarity;

}
