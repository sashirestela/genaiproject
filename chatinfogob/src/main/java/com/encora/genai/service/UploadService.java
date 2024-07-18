package com.encora.genai.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class UploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);

    private static UploadService uploadService = null;

    private UploadService() {
    }

    public static UploadService one() {
        if (uploadService == null) {
            uploadService = new UploadService();
        }
        return uploadService;
    }

    public void processPdf(String fullFileName) throws IOException {
        String fullText = read(fullFileName);
        List<Fragment> fragments = cleanAndSplitText(fullText);
        List<List<Double>> embeddings = generateEmbeddings(fragments);
        saveFragments(fragments, embeddings);
    }

    private String read(String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            LOGGER.debug("The " + fileName + " was read.");
            return text;
        }
    }

    private List<Fragment> cleanAndSplitText(String text) throws IOException {
        String cleanedText = Splitter.cleanText(text);
        return Splitter.splitByRegex(cleanedText);
    }

    private List<List<Double>> generateEmbeddings(List<Fragment> fragments) {
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

    private void saveFragments(List<Fragment> fragments, List<List<Double>> embeddings) {
        Database.prepareDatabase();
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setEmbedding(embeddings.get(i));
        }
        Database.insertSegments(fragments);
    }

}
