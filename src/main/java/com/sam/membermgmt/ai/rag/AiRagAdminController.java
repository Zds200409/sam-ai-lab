package com.sam.membermgmt.ai.rag;

import com.sam.membermgmt.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "RAG 知识库管理")
@RestController
@RequestMapping("/api/ai/admin")
@RequiredArgsConstructor
public class AiRagAdminController {

    private final KnowledgeIngestService ingestService;
    private final VectorStore vectorStore;

    @Operation(summary = "重建知识库索引")
    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(ingestService.ingest());
    }

    @Operation(summary = "检索测试", description = "阈值设 0，观察真实分数分布")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "3") int topK) {

        SearchRequest req = SearchRequest.builder()
                .query(q)
                .topK(topK)
                .similarityThreshold(0.0)
                .build();

        List<Document> docs = vectorStore.similaritySearch(req);

        return Result.success(docs.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", d.getMetadata().getOrDefault("title", ""));
            m.put("category", d.getMetadata().getOrDefault("category", ""));
            m.put("score", d.getScore());
            m.put("preview", d.getText().length() > 60
                    ? d.getText().substring(0, 60)
                    : d.getText());
            return m;
        }).toList());
    }
}

