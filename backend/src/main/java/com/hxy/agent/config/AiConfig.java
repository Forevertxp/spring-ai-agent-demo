package com.hxy.agent.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiConfig {

    @Bean
    public Generation generation() {
        return new Generation();
    }
}