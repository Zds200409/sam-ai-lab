package com.sam.membermgmt.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员章程切分器。
 *
 * 章程是两级结构：二级标题 "## 第五章 商品退换货" 是章，三级标题 "### 5.9 退款金额与到账时间" 是节。
 * 按章切会得到七个九百多字的巨块，每块混着十几条政策，向量被平均掉，
 * 问具体细节时分数上不去。所以按节切，一块一个政策点，章名作为元数据保留。
 *
 * 和 FaqChunker 的区别只在切分依据：FAQ 一条就是一个 "## "，章程的 "## " 是章不是条目。
 * 元数据约定两边保持一致（category / source 解析后从正文剥离），改约定时两处要同步。
 */
@Component
public class PolicyChunker {

    private static final String PREFIX_CHAPTER = "## ";
    private static final String PREFIX_SECTION = "### ";
    private static final String PREFIX_CATEGORY = "category:";
    private static final String PREFIX_SOURCE = "source:";
    private static final int MIN_BODY_LENGTH = 8;

    public List<Document> chunk(String raw) {
        List<Document> out = new ArrayList<>();
        String chapter = "";
        String title = null;
        List<String> body = new ArrayList<>();

        for (String line : raw.split("\n")) {
            if (line.startsWith(PREFIX_CHAPTER)) {
                appendSection(out, chapter, title, body);
                chapter = line.substring(PREFIX_CHAPTER.length()).trim();
                title = null;
                body = new ArrayList<>();
            } else if (line.startsWith(PREFIX_SECTION)) {
                appendSection(out, chapter, title, body);
                title = line.substring(PREFIX_SECTION.length()).trim();
                body = new ArrayList<>();
            } else if (title != null) {
                body.add(line);
            }
        }
        appendSection(out, chapter, title, body);
        return out;
    }

    private void appendSection(List<Document> out, String chapter, String title, List<String> bodyLines) {
        if (title == null) {
            return;
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("title", title);
        meta.put("chapter", chapter);
        meta.put("doc_type", "policy");
        meta.put("category", "");
        meta.put("source", "");

        StringBuilder content = new StringBuilder();
        for (String line : bodyLines) {
            String t = line.trim();
            if (t.startsWith(PREFIX_CATEGORY)) {
                meta.put("category", t.substring(PREFIX_CATEGORY.length()).trim());
                continue;
            }
            if (t.startsWith(PREFIX_SOURCE)) {
                meta.put("source", t.substring(PREFIX_SOURCE.length()).trim());
                continue;
            }
            content.append(line).append("\n");
        }

        String text = title + "\n" + content.toString().trim();
        if (text.length() < title.length() + MIN_BODY_LENGTH) {
            return;
        }
        out.add(new Document(buildId(title), text, meta));
    }

    /**
     * 用条款号做文档 id，例如 "### 5.9 退款金额与到账时间" 得到 policy-5.9。
     * id 稳定，重复 reindex 时 Same id 会被覆盖而不是追加。
     */
    private String buildId(String title) {
        int space = title.indexOf(' ');
        String articleNo = space > 0 ? title.substring(0, space) : title;
        return "policy-" + articleNo;
    }
}
