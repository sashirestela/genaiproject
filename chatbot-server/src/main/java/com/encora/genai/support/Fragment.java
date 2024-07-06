package com.encora.genai.support;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Fragment {

    private Long id;
    private String content;
    private List<Double> embedding;

}
