package com.sam.membermgmt.ai;

import com.sam.membermgmt.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class AiAssistantService {

    private static final String DEFAULT_CONVERSATION_ID = "demo";

    private final ChatClient chatClient;
    private final Duration timeout;
    private final String systemPrompt;

    public AiAssistantService(ChatClient.Builder chatClientBuilder,
                              ChatMemory chatMemory,
                              VectorStore vectorStore,
                              @Value("${sam.ai.timeout-seconds:30}") long timeoutSeconds,
                              @Value("${sam.ai.system-prompt:}") String systemPrompt) {

        // 查询改写用的 builder 必须独立。它不能带上后面的记忆和检索 advisor，
        // 否则改写器调用时会再次触发自身，形成递归。
        ChatClient.Builder compressorBuilder = chatClientBuilder.clone();

        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                        CompressionQueryTransformer.builder()
                                .chatClientBuilder(compressorBuilder)
                                .build())
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .similarityThreshold(0.5)
                                .topK(3)
                                .build())
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                .allowEmptyContext(false)
                                .build())
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor)
                .build();

        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.systemPrompt = systemPrompt;
    }

    private String resolveConversationId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? DEFAULT_CONVERSATION_ID
                : conversationId.trim();
    }

    public Result<String> chat(String question, String conversationId) {
        if (question == null || question.trim().isEmpty()) {
            return Result.error(400, "问题不能为空");
        }
        String cid = resolveConversationId(conversationId);
        long start = System.currentTimeMillis();
        try {
            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                    .call()
                    .content();
            long cost = System.currentTimeMillis() - start;
            log.info("[AI-ASK] cost={}ms threshold={}ms at={}", cost, timeout.toMillis(), Instant.now());
            if (answer == null || answer.isBlank()) {
                return Result.error(502, "模型返回内容为空，请重试");
            }
            return Result.success(answer);
        } catch (ResourceAccessException e) {
            log.error("[AI-ASK] 模型服务不可达或超时: {}", e.getMessage());
            return Result.error(503, "智能助手响应超时或服务不可用，请稍后重试");
        } catch (Exception e) {
            log.error("[AI-ASK] 模型调用异常", e);
            return Result.error(500, "智能助手处理异常，请稍后重试");
        }
    }

    public Flux<String> chatStream(String question, String conversationId) {
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("[错误] 问题不能为空");
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                        resolveConversationId(conversationId)))
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("[AI-STREAM] 流式调用异常", e);
                    return Flux.just("[降级] 模型服务暂时不可用，已切换为兜底响应");
                });
    }
}
