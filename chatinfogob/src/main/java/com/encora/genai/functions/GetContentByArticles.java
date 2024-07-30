package com.encora.genai.functions;

import java.util.List;

import com.encora.genai.support.Database;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.sashirestela.openai.common.function.Functional;

@JsonClassDescription("Consulta por el contenido de una lista de articulos de la Constitucion Politica del Peru.")
public class GetContentByArticles implements Functional {

    public static final String DESCRIPTION = "Consulta por el contenido de una lista de articulos de la Constitucion Politica del Peru.";

    @JsonProperty(required = true)
    public List<Integer> articles;

    @Override
    public Object execute() {
        return Database.selectFragments(articles);
    }

}
