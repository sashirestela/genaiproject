package com.encora.genai.support;

public class Commons {

    private Commons() {
    }

    public static final String FIELD_SEPARATOR = " || ";
    public static final String FIELD_SEPARATOR_REGEX = " \\|\\| ";

    public static final String START_TEXT_FOR_LAST_PART = "Artículo";
    
    public static final String CONTENT_SEPARATOR = " : ";
    
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
