package com.encora.genai.data;

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

}
