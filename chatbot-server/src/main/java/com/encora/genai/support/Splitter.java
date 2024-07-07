package com.encora.genai.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.Fragment;

public class Splitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Splitter.class);
    private static final int MAX_INDEX_LEVEL = 2;
    private static final String FIELD_SEPARATOR = "\n";
    private static final String REFERENCE_SEPARATOR = " || ";
    private static final String LEVEL_JOINNER = " - ";
    private static final String REGEX_TO_CLEAN = "([^A-Z\\.\\:])\\n((?![a-z]\\. )[a-z])";
    private static final String[] REGEX_BY_LEVEL = {
            "(PRE.MBULO|T.TULO.*\\n.*|DISPOSICIONES.*|DECLARACI.N.*\\n.*)",
            "(CAP.TULO.*\\n.*|\\n?[A-Z][a-záéíóúñ]*\\.-\\s)",
            "(Art.culo.*\\.--|Art.culo.*\\.*)"
    };

    public static List<Fragment> splitByRegex(String text) {
        List<Fragment> fragments = new ArrayList<>();
        splitTextByRegex(0, "", text, fragments);
        LOGGER.debug("Text was splitted by regex");
        return fragments;
    }

    public static String cleanText(String text) {
        Pattern pattern = Pattern.compile(REGEX_TO_CLEAN);
        Matcher matcher = pattern.matcher(text);
        String cleanedText = matcher.replaceAll("$1 $2");
        return cleanedText;
    }

    private static void splitTextByRegex(int level, String previous, String text, List<Fragment> fragments) {
        Pattern pattern = Pattern.compile(REGEX_BY_LEVEL[level]);
        Matcher levelMatcher = pattern.matcher(text);
        String[] innerTexts = text.split(REGEX_BY_LEVEL[level], 0);
        String firstText = innerTexts[0];
        if (!firstText.isBlank()) {
            splitOrAdd(level, previous, "", firstText, fragments);
        }
        for (int i = 1; i < innerTexts.length; i++) {
            String innerText = innerTexts[i];
            String levelText = levelMatcher.find()
                    ? levelMatcher.group().trim().replaceAll("\\n", LEVEL_JOINNER)
                    : "";
            splitOrAdd(level, previous, levelText, innerText, fragments);
        }
    }

    private static void splitOrAdd(int level, String previous, String levelText, String innerText,
            List<Fragment> fragments) {
        String previousText = levelText.isEmpty() ? previous : previous + levelText + FIELD_SEPARATOR;
        if (level < MAX_INDEX_LEVEL) {
            splitTextByRegex(level + 1, previousText, innerText, fragments);
        } else {
            addFragment(previousText, innerText.trim(), fragments);
        }
    }

    private static void addFragment(String previous, String innerText, List<Fragment> fragments) {
        if (previous.length() + innerText.length() <= Commons.MAX_NUM_CHARS_PER_FRAGMENT) {
            fragments.add(newFragment(previous, innerText));
        } else {
            int pivotIndex = innerText.lastIndexOf("\n", Commons.MAX_NUM_CHARS_PER_FRAGMENT - previous.length());
            fragments.add(newFragment(previous, innerText.substring(0, pivotIndex)));
            addFragment(previous, innerText.substring(pivotIndex + 1), fragments);
        }
    }

    private static Fragment newFragment(String previous, String innerText) {
        return Fragment.builder()
                .reference(previous.trim().replaceAll(FIELD_SEPARATOR, REFERENCE_SEPARATOR))
                .content(innerText)
                .build();
    }

}
