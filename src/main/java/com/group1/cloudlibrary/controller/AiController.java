package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.ChatDTO;
import com.group1.cloudlibrary.service.AiService;
import com.group1.cloudlibrary.util.UserContext;
import com.group1.cloudlibrary.vo.ChatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力接口（用户端）：智能问答
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** 智能问答（RAG：知识库检索 + 大模型生成，带多轮上下文） */
    @PostMapping("/chat")
    public Result<ChatVO> chat(@Valid @RequestBody ChatDTO dto) {
        return Result.success(aiService.chat(dto.getQuestion()));
    }

    /** 清空当前用户的多轮对话上下文（前端「清空对话」按钮调用） */
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        Long userId = UserContext.getUserId();
        aiService.clearHistory(userId == null ? null : String.valueOf(userId));
        return Result.success(null);
    }
}
