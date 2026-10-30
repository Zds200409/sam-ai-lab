package com.sam.membermgmt.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private final VectorStore vectorStore;
    private final FaqChunker chunker;
    private final ResourceLoader resourceLoader;

    public int ingest() {
        Resource res = resourceLoader.getResource("classpath:knowledge/sam-faq.md");

        String raw;
        try (InputStream in = res.getInputStream()) {
            raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("FAQ 文件读取失败: " + e.getMessage(), e);
        }

        List<Document> docs = chunker.chunk(raw);
        vectorStore.add(docs);
        log.info("[INGEST] 写入 {} 条 FAQ", docs.size());
        return docs.size();
    }
}

