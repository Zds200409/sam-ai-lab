package com.sam.membermgmt.config;



import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${sam.ai.vector-store.path:data/vector-store.json}")
    private String storePath;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        File file = new File(storePath);
        if (file.exists() && file.length() > 0) {
            try {
                store.load(file);
                log.info("[VSTORE] 已从 {} 加载向量库", file.getAbsolutePath());
                return store;
            } catch (Exception e) {
                log.warn("[VSTORE] 加载失败，回退为空库: {}", e.getMessage());
            }
        }
        log.info("[VSTORE] 无可用持久化文件，向量库为空，请调用 /api/ai/admin/reindex");
        return store;
    }
}


