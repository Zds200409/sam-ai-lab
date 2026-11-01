package com.sam.membermgmt.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private final SimpleVectorStore vectorStore;
    private final FaqChunker chunker;
    private final ResourceLoader resourceLoader;

    @Value("${sam.ai.vector-store.path:data/vector-store.json}")
    private String storePath;

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

        persist();
        return docs.size();
    }

    private void persist() {
        File file = new File(storePath);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            log.warn("[INGEST] 目录创建失败: {}", dir.getAbsolutePath());
            return;
        }
        try {
            vectorStore.save(file);
            log.info("[INGEST] 向量库已落盘: {} ({} 字节)", file.getAbsolutePath(), file.length());
        } catch (Exception e) {
            log.warn("[INGEST] 落盘失败: {}", e.getMessage());
        }
    }
}
