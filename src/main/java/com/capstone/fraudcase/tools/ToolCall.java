package com.capstone.fraudcase.tools;

import java.util.Map;

public record ToolCall(String toolName, Map<String, Object> arguments) {
}
