package com.example.rag.service;

import com.example.rag.api.dto.UploadResponse;
import com.example.rag.model.DocumentChunk;
import com.example.rag.vector.VectorStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    public DocumentIngestionService(
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker,
            VectorStore vectorStore
    ) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
    }

    public UploadResponse ingest(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        String documentName = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        if (!documentName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }

        String documentId = UUID.randomUUID().toString();
        String text = pdfTextExtractor.extract(file.getInputStream());
        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "The PDF contains no extractable text. Scanned image-only PDFs are not supported.");
        }
        List<String> chunks = textChunker.split(text);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("The PDF could not be split into searchable text chunks.");
        }

        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            documentChunks.add(new DocumentChunk(
                    UUID.randomUUID().toString(),
                    documentId,
                    documentName,
                    i,
                    chunks.get(i)
            ));
        }

        vectorStore.upsert(documentChunks);
        return new UploadResponse(documentId, documentName, documentChunks.size());
    }
}
