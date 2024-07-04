package com.encora.genai.chatbot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import lombok.Builder;
import lombok.Data;

public class App {

    private static final int TOTAL_LEVELS = 3;
    private static final String SEPARATOR = "|";
    private static final String REGEX_BAD_BREAKLINES = "([^A-Z\\.\\:])\\n((?![a-z]\\. )[a-z])";
    private static final List<String> REGEX_BY_LEVEL = List.of(
            "(PRE.MBULO|T.TULO.*\\n.*|DISPOSICIONES.*|DECLARACI.N.*\\n.*)",
            "(CAP.TULO.*\\n.*|\\n?[A-Z][a-záéíóúñ]*\\.-)",
            "(Art.culo.*\\.--|Art.culo.*\\.*|Art.culo.*-[AB].*:.|Art.culo.*-[AB].)",
            "\\n\\s*\\d+\\.\\s",
            "\\n\\s*\\D\\.\\s");

    String fullText;
    StringBuffer splittedText;

    public App() {
        splittedText = new StringBuffer();
    }

    public void readTextFromPdf(String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            fullText = pdfStripper.getText(document);
        }
    }

    public void removeBreaklines() {
        Pattern pattern = Pattern.compile(REGEX_BAD_BREAKLINES);
        Matcher matcher = pattern.matcher(fullText);
        fullText = matcher.replaceAll("$1 $2");
    }

    public void saveTextToFile(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(fullText);
        writer.close();
    }

    public void splitTextByRegex(int level, String text, String previous) {
        Pattern pattern = Pattern.compile(REGEX_BY_LEVEL.get(level));
        Matcher levelMatcher = pattern.matcher(text);
        List<String> textList = pattern.splitAsStream(text).collect(Collectors.toList());
        String firstText = textList.get(0);
        if (!firstText.isBlank()) {
            if (level < TOTAL_LEVELS) {
                splitTextByRegex(level + 1, firstText, previous + SEPARATOR);
            } else {
                this.splittedText.append(previous + SEPARATOR + firstText.trim() + "\n");
            }
        }
        for (int i = 1; i < textList.size(); i++) {
            String textByMatch = textList.get(i);
            String levelText = levelMatcher.find() ? levelMatcher.group().trim() : "";
            if (level < TOTAL_LEVELS) {
                splitTextByRegex(level + 1, textByMatch, previous + levelText + SEPARATOR);
            } else {
                this.splittedText.append(previous + levelText + SEPARATOR + textByMatch.trim() + "\n");
            }
        }
    }

    public void saveSplittedToCsv(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(splittedText.toString());
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        String pdfFileName = "/projects/genaiproject/documents/constitucion_politica_peru_2024.pdf";
        String txtFileName = "/projects/genaiproject/documents/constitucion_politica_peru_2024.txt";
        String csvFileName = "/projects/genaiproject/documents/constitucion_politica_peru_2024.out";

        App app = new App();
        app.readTextFromPdf(pdfFileName);
        app.removeBreaklines();
        app.saveTextToFile(txtFileName);
        app.splitTextByRegex(0, app.fullText, "");
        app.saveSplittedToCsv(csvFileName);
    }

    @Data
    @Builder
    public static class DocumentPart {
        private String title;
        private String chapter;
        private String article;
        private String numeral;
        private String literal;
        private String content;
    }
}
