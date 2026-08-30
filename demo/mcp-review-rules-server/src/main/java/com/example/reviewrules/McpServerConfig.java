package com.example.reviewrules;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider reviewRulesTools(ReviewRulesService reviewRulesService) {
        return MethodToolCallbackProvider.builder().toolObjects(reviewRulesService).build();
    }
}
