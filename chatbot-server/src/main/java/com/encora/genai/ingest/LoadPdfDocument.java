package com.encora.genai.ingest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.support.Database;
import com.encora.genai.support.Fragment;
import com.encora.genai.support.GenerativeAI;

public class LoadPdfDocument {

    public static void main(String[] args) throws IOException {
        String pdfFileName = "/projects/genaiproject/documents/constitucion_politica_peru_2024.pdf";
        String txtFileName = pdfFileName.split("\\.")[0] + ".txt";

        LoadPdfDocument app = new LoadPdfDocument();
        app.readPdf(pdfFileName);
        app.removeBreaklines();
        app.splitText();
        app.generateEmbeddings();
        app.saveToFile(app.fullText, txtFileName);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadPdfDocument.class);
    private static final int TOTAL_LEVELS = 3;
    private static final String FIELD_SEPARATOR = "\n";
    private static final String LEVEL_JOINNER = " - ";
    private static final String REGEX_BAD_BREAKLINES = "([^A-Z\\.\\:])\\n((?![a-z]\\. )[a-z])";
    private static final String[] REGEX_BY_LEVEL = {
            "(PRE.MBULO|T.TULO.*\\n.*|DISPOSICIONES.*|DECLARACI.N.*\\n.*)",
            "(CAP.TULO.*\\n.*|\\n?[A-Z][a-záéíóúñ]*\\.-)",
            "(Art.culo.*\\.--|Art.culo.*\\.*|Art.culo.*-[AB].*:.|Art.culo.*-[AB].)",
            "\\n\\s*\\d+\\.\\s",
            "\\n\\s*\\D\\.\\s"
    };
    private static final int MAX_LENGTH_TO_SEND = 5_000;

    String fullText;
    List<String> contents;
    List<List<Double>> embeddings;

    public LoadPdfDocument() {
        fullText = "";
        contents = new ArrayList<>();
        embeddings = new ArrayList<>();
    }

    public void readPdf(String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            fullText = pdfStripper.getText(document);
            LOGGER.debug("The " + fileName + " was read.");
        }
    }

    public void removeBreaklines() {
        Pattern pattern = Pattern.compile(REGEX_BAD_BREAKLINES);
        Matcher matcher = pattern.matcher(fullText);
        fullText = matcher.replaceAll("$1 $2");
        LOGGER.debug("All breaklines were removed.");
    }

    public void splitText() {
        splitTextByRegex(0, "", fullText);
        LOGGER.debug("Full text was splitted.");
    }

    private void splitTextByRegex(int level, String previous, String text) {
        Pattern pattern = Pattern.compile(REGEX_BY_LEVEL[level]);
        Matcher levelMatcher = pattern.matcher(text);
        String[] innerTexts = text.split(REGEX_BY_LEVEL[level], 0);
        String firstText = innerTexts[0];
        if (!firstText.isBlank()) {
            splitOrAppend(level, previous, "", firstText);
        }
        for (int i = 1; i < innerTexts.length; i++) {
            String innerText = innerTexts[i];
            String levelText = levelMatcher.find()
                    ? levelMatcher.group().trim().replaceAll("\\n", LEVEL_JOINNER)
                    : "";
            splitOrAppend(level, previous, levelText, innerText);
        }
    }

    private void splitOrAppend(int level, String previous, String levelText, String innerText) {
        String previousText = levelText.isEmpty() ? previous : previous + levelText + FIELD_SEPARATOR;
        if (level < TOTAL_LEVELS) {
            splitTextByRegex(level + 1, previousText, innerText);
        } else {
            contents.add(previousText + innerText.trim());
        }
    }

    public void saveToFile(String text, String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(text);
        writer.close();
    }

    public void generateEmbeddings() {
        Database.prepareDatabase();
        List<String> partialContents = new ArrayList<>();
        for (String content : contents) {
            int partialContentLength = partialContents.stream().mapToInt(String::length).sum();
            if (partialContentLength + content.length() > MAX_LENGTH_TO_SEND) {
                embeddings.addAll(GenerativeAI.createEmbeddings(partialContents));
                partialContents = new ArrayList<>();
                partialContents.add(content);
            } else {
                partialContents.add(content);
            }
        }
        embeddings.addAll(GenerativeAI.createEmbeddings(partialContents));
        List<Fragment> fragments = IntStream.range(0, contents.size())
                .mapToObj(i -> Fragment.builder()
                        .content(contents.get(i))
                        .embedding(embeddings.get(i))
                        .build())
                .toList();
        Database.insertSegments(fragments);
        //More memory is required
        //Database.indexSegments();
    }

}
