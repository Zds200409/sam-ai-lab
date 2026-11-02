package com.sam.membermgmt.ai.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;

/**
 * 可清空的向量库。
 *
 * SimpleVectorStore 没有提供清空接口，但它内部用 Map 存文档、且字段是 protected，
 * 所以继承出来暴露一个 clear()。
 *
 * 需要清空的理由：SimpleVectorStore.add() 是 Map.put 语义，按文档 id 覆盖。
 * 我们给每块知识分配了固定 id，正常重复 reindex 不会产生重复。
 * 但早期版本写入的文档用的是随机 UUID，那份历史数据清不掉，
 * 每次 reindex 都会留下一份旧的。先清空再写入可以让 reindex 完全幂等，
 * 不再依赖“记得删快照文件”这种口头约定。
 */
public class ResettableVectorStore extends SimpleVectorStore {

    protected ResettableVectorStore(SimpleVectorStoreBuilder builder) {
        super(builder);
    }

    public static ResettableVectorStore create(EmbeddingModel embeddingModel) {
        return new ResettableVectorStore(SimpleVectorStore.builder(embeddingModel));
    }

    /** 清空全部文档，仅用于重建索引 */
    public void clear() {
        this.store.clear();
    }
}
