package com.encora.genai.support;

import static com.encora.genai.support.Commons.MAX_INDEX_LEVEL;
import static com.encora.genai.support.Commons.MAX_NUM_CHARS;
import static com.encora.genai.support.Commons.TO_CLEAN_REGEX;
import static com.encora.genai.support.Commons.BY_LEVEL_REGEX;
import static com.encora.genai.support.Commons.EMPTY;
import static com.encora.genai.support.Commons.FIELD_SEPARATOR;
import static com.encora.genai.support.Commons.FIELD_SEPARATOR_REGEX;
import static com.encora.genai.support.Commons.LEVEL_JOINNER;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.Fragment;

public class Splitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Splitter.class);

    public static List<Fragment> splitByRegex(String text) {
        List<Fragment> fragments = new ArrayList<>();
        splitTextByRegex(0, "", text, fragments);
        LOGGER.debug("Text was splitted by regex");
        return fragments;
    }

    public static String cleanText(String text) {
        Pattern pattern = Pattern.compile(TO_CLEAN_REGEX);
        Matcher matcher = pattern.matcher(text);
        String cleanedText = matcher.replaceAll("$1 $2");
        return cleanedText;
    }

    private static void splitTextByRegex(int level, String previous, String text, List<Fragment> fragments) {
        Pattern pattern = Pattern.compile(BY_LEVEL_REGEX[level]);
        Matcher levelMatcher = pattern.matcher(text);
        String[] innerTexts = text.split(BY_LEVEL_REGEX[level], 0);
        String firstText = innerTexts[0];
        if (!firstText.isBlank()) {
            splitOrAdd(level, previous, EMPTY, firstText, fragments);
        }
        for (int i = 1; i < innerTexts.length; i++) {
            String innerText = innerTexts[i];
            String levelText = levelMatcher.find()
                    ? levelMatcher.group().trim().replaceAll("\\n", LEVEL_JOINNER)
                    : EMPTY;
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
        if (previous.length() + innerText.length() <= MAX_NUM_CHARS) {
            fragments.add(newFragment(previous, innerText));
        } else {
            int pivotIndex = innerText.lastIndexOf("\n", MAX_NUM_CHARS - previous.length());
            fragments.add(newFragment(previous, innerText.substring(0, pivotIndex)));
            addFragment(previous, innerText.substring(pivotIndex + 1), fragments);
        }
    }

    private static Fragment newFragment(String previous, String innerText) {
        Fragment fragment = Fragment.builder()
                .reference(previous.replaceAll(FIELD_SEPARATOR_REGEX + "$", ""))
                .content(innerText)
                .build();
        fragment.updateContentWithReference();
        return fragment;
    }

}
