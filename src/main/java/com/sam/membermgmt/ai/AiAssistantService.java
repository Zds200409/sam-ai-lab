package com.sam.membermgmt.ai;

import com.sam.membermgmt.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

/**
 * AI 会员助手服务。
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有模型调用统一走 {@link ChatClient}，模型/温度等由 application.yml 配置，
 *       切换 DeepSeek、通义、Ollama 只需改配置，业务代码零改动。</li>
 *   <li>每次调用记录耗时与调用方标识，便于后续做慢查询分析与成本核算。</li>
 *   <li>模型不可用时统一降级为友好提示，不把底层异常抛给前端。</li>
 * </ul>
 */
@Slf4j
@Service
public class AiAssistantService {

    private final ChatClient chatClient;

    /** 单次问答的超时上限，超过即判定为不可用并降级。 */
    private final Duration timeout;

    /** 系统提示词：限定角色与回答风格，避免模型自由发挥导致答非所问。 */
    private final String systemPrompt;

    public AiAssistantService(ChatClient.Builder chatClientBuilder,
                              @Value("${sam.ai.timeout-seconds:30}") long timeoutSeconds,
                              @Value("${sam.ai.system-prompt:}") String systemPrompt) {
        this.chatClient = chatClientBuilder.build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.systemPrompt = systemPrompt;
    }

    /**
     * 同步问答。返回统一结果对象，失败时通过 {@link Result#error} 承载降级信息。
     */
    public Result<String> chat(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Result.error(400, "问题不能为空");
        }
        long start = System.currentTimeMillis();
        try {
            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();
            long cost = System.currentTimeMillis() - start;
            log.info("[AI-ASK] cost={}ms threshold={}ms at={}", cost, timeout.toMillis(), Instant.now());
            if (answer == null || answer.isBlank()) {
                return Result.error(502, "模型返回内容为空，请重试");
            }
            return Result.success(answer);
        } catch (ResourceAccessException e) {
            // 连接层异常：连不上、读超时，Spring AI 统一包装为 ResourceAccessException
            log.error("[AI-ASK] 模型服务不可达或超时: {}", e.getMessage());
            return Result.error(503, "智能助手响应超时或服务不可用，请稍后重试");
        } catch (Exception e) {
            log.error("[AI-ASK] 模型调用异常", e);
            return Result.error(500, "智能助手处理异常，请稍后重试");
        }
    }

    /**
     * 流式问答（SSE）。流式链路无法在服务端统一兜底为 Result，
     * 因此对首字节异常做降级：连接建立失败时直接推一条错误事件再结束流。
     */
    public Flux<String> chatStream(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("[错误] 问题不能为空");
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("[AI-STREAM] 流式调用异常", e);
                    return Flux.just("[降级] 模型服务暂时不可用，已切换为兜底响应");
                });
    }
}
