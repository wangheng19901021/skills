package com.example.reviewrules;

import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

/**
 * Spring AI 2.0 的 DefaultToolCallResultConverter 会把 String 返回值也 JSON 序列化
 * （输出带引号和 \n 转义）。本转换器原样返回文本，让 MCP 客户端拿到可读的多行文本。
 */
public class PlainTextResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        return result == null ? "" : result.toString();
    }
}
