package com.sam.membermgmt.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FAQ 切分器。
 *
 * 知识库是结构化 markdown，每条一个 "## 标题"，所以按标题切，不用固定窗口。
 * 切分时做三件事：
 * 1. category / source 两行解析成元数据并从正文剥离，否则它们会被一起向量化，
 *    检索结果里也会漏出来给用户看到。
 * 2. 带 TODO 标记的条目标记为待确认，直接跳过不入库。未确认的政策进库之后
 *    模型会当作官方事实引用，比检索不到更危险。
 * 3. 正文过短的条目丢弃，避免产生只有标题的碎片。
 */
@Component
public class FaqChunker {

    private static final String PREFIX_CATEGORY = "category:";
    private static final String PREFIX_SOURCE = "source:";
    private static final String MARK_TODO = "TODO";
    private static final int MIN_BODY_LENGTH = 8;

    public List<Document> chunk(String raw) {
        List<Document> out = new ArrayList<>();
        String[] blocks = raw.split("(?m)^## ");

        for (String block : blocks) {
            if (block.isBlank()) continue;
            // 第一个分块是文件头大标题和说明段，不以 "## " 开头，这里排掉
            if (block.startsWith("# ")) continue;

            int nl = block.indexOf('\n');
            if (nl < 0) continue;

            String title = block.substring(0, nl).trim();
            String body = block.substring(nl + 1);

            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("category", "");
            meta.put("source", "");

            StringBuilder content = new StringBuilder();
            boolean reviewRequired = false;

            for (String line : body.split("\n")) {
                String t = line.trim();
                if (t.startsWith(PREFIX_CATEGORY)) {
                    meta.put("category", t.substring(PREFIX_CATEGORY.length()).trim());
                    continue;
                }
                if (t.startsWith(PREFIX_SOURCE)) {
                    meta.put("source", t.substring(PREFIX_SOURCE.length()).trim());
                    continue;
                }
                if (t.startsWith(MARK_TODO)) {
                    reviewRequired = true;
                    continue;
                }
                content.append(line).append("\n");
            }

            // 待确认内容不入库，等人工填完再索引
            if (reviewRequired) continue;

            String text = title + "\n" + content.toString().trim();
            if (text.length() < title.length() + MIN_BODY_LENGTH) continue;

            out.add(new Document(text, meta));
        }
        return out;
    }
}
