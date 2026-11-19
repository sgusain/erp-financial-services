package com.erp.ai.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.erp.ai.tool.ErpReadTools;

/**
 * Makes our tools available to other AI apps (Claude Desktop, Claude Code, an MCP inspector) at /mcp.
 * Only READ tools are exposed. The write tool is not, on purpose. Callers must send a JWT.
 */
@Configuration
public class McpServerConfig {

    @Bean
    ToolCallbackProvider erpMcpTools(ErpReadTools readTools) {
        return MethodToolCallbackProvider.builder().toolObjects(readTools).build();
    }
}
