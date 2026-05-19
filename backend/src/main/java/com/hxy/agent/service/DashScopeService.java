package com.hxy.agent.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DashScopeService {

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    @Value("${spring.ai.alibaba.chat.options.model:qwen-plus}")
    private String model;

    private final Generation generation;

    private final Map<String, List<Message>> sessionMessages = new HashMap<>();

    public DashScopeService(Generation generation) {
        this.generation = generation;
    }

    public String chat(String sessionId, String systemPrompt, String userMessage) {
        try {
            List<Message> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());

            if (messages.isEmpty() && systemPrompt != null) {
                messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
            }

            messages.add(Message.builder().role(Role.USER.getValue()).content(userMessage).build());

            GenerationParam param = GenerationParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .messages(messages)
                    .temperature(0.7f)
                    .build();

            GenerationResult result = generation.call(param);

            String response = result.getOutput().getChoices().get(0).getMessage().getContent();

            messages.add(Message.builder().role(Role.ASSISTANT.getValue()).content(response).build());

            log.info("AI响应: {}", response);
            return response;

        } catch (NoApiKeyException | ApiException | InputRequiredException e) {
            log.error("调用DashScope失败: {}", e.getMessage());
            return "抱歉，AI服务暂时不可用: " + e.getMessage();
        }
    }

    public Flux<String> chatStream(String sessionId, String systemPrompt, String userMessage) {
        return Flux.create(emitter -> {
            try {
                List<Message> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());

                if (messages.isEmpty() && systemPrompt != null) {
                    messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
                }

                messages.add(Message.builder().role(Role.USER.getValue()).content(userMessage).build());

                GenerationParam param = GenerationParam.builder()
                        .model(model)
                        .apiKey(apiKey)
                        .messages(messages)
                        .temperature(0.7f)
                        .incrementalOutput(true)
                        .build();

                generation.streamCall(param, new ResultCallback<GenerationResult>() {
                    private StringBuilder fullResponse = new StringBuilder();

                    @Override
                    public void onEvent(GenerationResult result) {
                        String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                        if (content != null && !content.isEmpty()) {
                            fullResponse.append(content);
                            emitter.next(content);
                        }
                    }

                    @Override
                    public void onError(Exception err) {
                        log.error("流式调用错误: {}", err.getMessage());
                        emitter.error(err);
                    }

                    @Override
                    public void onComplete() {
                        messages.add(Message.builder().role(Role.ASSISTANT.getValue()).content(fullResponse.toString()).build());
                        emitter.complete();
                    }
                });

            } catch (NoApiKeyException | ApiException | InputRequiredException e) {
                log.error("流式调用失败: {}", e.getMessage());
                emitter.error(e);
            }
        });
    }

    public void clearSession(String sessionId) {
        sessionMessages.remove(sessionId);
    }
}