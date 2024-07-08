package com.encora.genai.support;

public class Commons {

    private Commons() {
    }

    public static final String REFERENCE_SEPARATOR = " || ";
    public static final String REFERENCE_SEPARATOR_FOR_SPLIT = " \\|\\| ";
    public static final String EMPTY = "";
    public static final int MAX_INDEX_LEVEL = 2;
    public static final int MAX_NUM_CHARS_PER_FRAGMENT = 5_000;

    public static String getUserInput(String message, String defaultValue) {
        String input = System.console().readLine(message);
        if (input == null || input.isBlank()) {
            input = defaultValue;
        }
        return input;
    }

}
