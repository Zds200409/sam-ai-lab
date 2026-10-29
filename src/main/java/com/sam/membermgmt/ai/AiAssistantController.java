package com.sam.membermgmt.ai;

import com.sam.membermgmt.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 会员助手接口。
 *
 * <p>作为 sam-ai-lab 项目的第一个 AI 能力，演示三件事：
 * 同步问答、流式问答、以及模型不可用时的优雅降级。
 */
@Tag(name = "AI 会员助手")
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Operation(summary = "同步问答", description = "调用大模型同步返回答案，模型不可用时返回降级提示")
    @GetMapping("/chat")
    public Result<String> chat(@RequestParam String question,
                               @RequestParam(required = false) String conversationId) {
        return aiAssistantService.chat(question, conversationId);

    }


    @Operation(summary = "流式问答", description = "SSE 流式返回，逐字输出模型响应")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String question,
                                   @RequestParam(required = false) String conversationId) { // ★ 新增参数
        return aiAssistantService.chatStream(question, conversationId);                        // ★ 多传一个值
    }

}
