package com.encora.genai.ingest;

import java.util.List;

import com.encora.genai.support.Database;
import com.encora.genai.support.Fragment;
import com.encora.genai.support.GenerativeAI;

public class QueryContent {

    public static void main(String[] args) {
        String query = System.console().readLine("\nConsulta: ");
        String matchCount = System.console().readLine("Cantidad de resultados: ");
        List<Double> queryEmbedding = GenerativeAI.createEmbedding(query);
        List<Fragment> fragments = Database.selectFragments(queryEmbedding, Integer.parseInt(matchCount));
        fragments.forEach(fr -> System.out.println("\n" + fr.getContent()));
    }

}
