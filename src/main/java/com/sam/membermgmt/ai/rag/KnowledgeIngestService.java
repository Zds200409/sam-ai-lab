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
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库入库服务。
 *
 * 两种语料、两套切分：
 * - sam-faq.md 是结构化问答，一条一个二级标题，用 FaqChunker 按条切
 * - sam-charter.md 是会员章程，二级标题是章、三级标题是节，用 PolicyChunker 按节切
 *
 * 两块内容共用一个向量库，靠元数据 doc_type 区分（faq / policy），
 * 后续做混合检索时可以按类型分流，避免章程块把精准的问答块挤出 topK。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private static final String FAQ_PATH = "classpath:knowledge/sam-faq.md";
    private static final String POLICY_PATH = "classpath:knowledge/sam-charter.md";

    private final ResettableVectorStore vectorStore;
    private final FaqChunker faqChunker;
    private final PolicyChunker policyChunker;
    private final ResourceLoader resourceLoader;

    @Value("${sam.ai.vector-store.path:data/vector-store.json}")
    private String storePath;

    public int ingest() {
        List<Document> faqDocs = faqChunker.chunk(read(FAQ_PATH));
        List<Document> policyDocs = policyChunker.chunk(read(POLICY_PATH));

        List<Document> all = new ArrayList<>(faqDocs.size() + policyDocs.size());
        all.addAll(faqDocs);
        all.addAll(policyDocs);

        // 清空后重写。文档 id 固定，理论上 add 是覆盖语义，
        // 但清空能一并清掉早期版本用随机 id 写入的历史文档，
        // 让 reindex 的结果不依赖于「之前跑过几次」。
        vectorStore.clear();
        vectorStore.add(all);

        log.info("[INGEST] FAQ {} 条 + 章程 {} 节 = 共 {} 条", faqDocs.size(), policyDocs.size(), all.size());

        persist();
        return all.size();
    }

    private String read(String location) {
        Resource res = resourceLoader.getResource(location);
        try (InputStream in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("知识库文件读取失败: " + location + " -> " + e.getMessage(), e);
        }
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
