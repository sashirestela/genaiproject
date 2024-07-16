package com.encora.genai.ingest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.Fragment;
import com.encora.genai.support.Commons;
import com.encora.genai.support.Database;
import com.encora.genai.support.GenerativeAI;
import com.encora.genai.support.Splitter;

public class LoadPdfDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadPdfDocument.class);

    public static void main(String[] args) throws IOException {
        LoadPdfDocument app = new LoadPdfDocument();
        var fileName = args[0];
        var fullText = app.readPdf(fileName);
        var fragments = app.cleanAndSplitText(fullText);
        var embeddings = app.generateEmbeddings(fragments);
        app.saveFragments(fragments, embeddings);
    }

    public LoadPdfDocument() {
    }

    public void process(String fullFileName) throws IOException {
        String fullText = readPdf(fullFileName);
        List<Fragment> fragments = cleanAndSplitText(fullText);
        List<List<Double>> embeddings = generateEmbeddings(fragments);
        saveFragments(fragments, embeddings);
    }

    public String readPdf(String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            LOGGER.debug("The " + fileName + " was read.");
            return text;
        }
    }

    public List<Fragment> cleanAndSplitText(String text) throws IOException {
        String cleanedText = Splitter.cleanText(text);
        return Splitter.splitByRegex(cleanedText);
    }

    public List<List<Double>> generateEmbeddings(List<Fragment> fragments) {
        List<List<Double>> embeddings = new ArrayList<>();
        List<String> partialContents = new ArrayList<>();
        for (Fragment fragment : fragments) {
            int partialContentLength = partialContents.stream().mapToInt(String::length).sum();
            if (partialContentLength + fragment.getContent().length() > Commons.MAX_NUM_CHARS) {
                embeddings.addAll(GenerativeAI.createEmbeddings(partialContents));
                partialContents = new ArrayList<>();
                partialContents.add(fragment.getContent());
            } else {
                partialContents.add(fragment.getContent());
            }
        }
        embeddings.addAll(GenerativeAI.createEmbeddings(partialContents));
        return embeddings;
    }

    public void saveFragments(List<Fragment> fragments, List<List<Double>> embeddings) {
        Database.prepareDatabase();
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setEmbedding(embeddings.get(i));
        }
        Database.insertSegments(fragments);
    }

    public void saveOutputFile(List<Fragment> fragments, String fileName) throws IOException {
        String splittedText = fragments.stream()
                .map(fr -> fr.getReference() + "\n" + fr.getContent())
                .collect(Collectors.joining("\n\n"));
        String outFileName = fileName.split("\\.")[0] + ".out";
        saveToFile(splittedText, outFileName);
    }

    private void saveToFile(String text, String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(text);
        writer.close();
    }

}
