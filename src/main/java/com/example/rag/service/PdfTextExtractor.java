package com.example.rag.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractor {

    public String extract(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length < 5 || !startsWithPdfHeader(bytes)) {
            throw new IllegalArgumentException("Uploaded file is not a valid PDF.");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return normalize(stripper.getText(document));
        }
    }

    private boolean startsWithPdfHeader(byte[] bytes) {
        return "%PDF-".equals(new String(bytes, 0, 5, StandardCharsets.US_ASCII));
    }

    private String normalize(String text) {
        return text
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
