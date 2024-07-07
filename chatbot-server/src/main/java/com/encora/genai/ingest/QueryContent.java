package com.encora.genai.ingest;

import java.util.List;

import com.encora.genai.data.FragmentResult;
import com.encora.genai.support.Database;
import com.encora.genai.support.GenerativeAI;

public class QueryContent {

    public static void main(String[] args) {
        String query;
        Double matchThreshold;
        Integer matchCount;

        while (!(query = getUserInput("Escriba su consulta (presione 'Enter' para finalizar): ", "")).isEmpty()) {
            matchThreshold = Double.parseDouble(getUserInput("Umbral de coincidencia (0.45 por defecto): ", "0.45"));
            matchCount = Integer.parseInt(getUserInput("Cantidad de resultados (3 por defecto): ", "3"));

            List<Double> queryEmbedding = GenerativeAI.createEmbedding(query);
            List<FragmentResult> fragments = Database.selectFragments(queryEmbedding, matchThreshold, matchCount);

            fragments.forEach(fr -> System.out.println(""
                    + "-".repeat(50) + "\n"
                    + "Similaridad: " + fr.getSimilarity() + "\n"
                    + "Reference: " + fr.getReference() + "\n"
                    + fr.getContent()));
        }
    }

    private static String getUserInput(String message, String defaultValue) {
        String input = System.console().readLine(message);
        if (input == null || input.isBlank()) {
            input = defaultValue;
        }
        return input;
    }

}
