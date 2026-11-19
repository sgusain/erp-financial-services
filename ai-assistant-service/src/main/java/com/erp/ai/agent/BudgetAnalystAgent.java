package com.erp.ai.agent;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erp.ai.advisor.TokenUsageAdvisor;
import com.erp.ai.config.AiProperties;
import com.erp.ai.config.PromptLoader;
import com.erp.ai.dto.AgentResult;
import com.erp.ai.exception.LlmUnavailableException;
import com.erp.ai.limits.AiGuard;
import com.erp.ai.service.ErpDataService;
import com.erp.ai.service.LlmGateway;
import com.erp.ai.service.PendingActionService;

/**
 * An agent is a model that can call tools in a loop until it decides it is done.
 * Spring AI runs the loop. We add three safety limits:
 * a step limit (BudgetAnalystTools), a time limit (circuit breaker time limiter), and a human
 * confirmation for any change (PendingActionService).
 */
@Service
public class BudgetAnalystAgent {

    private static final Logger log = LoggerFactory.getLogger(BudgetAnalystAgent.class);

    private final ChatClient chatClient;
    private final PromptLoader prompts;
    private final ErpDataService erp;
    private final PendingActionService pendingActions;
    private final TokenUsageAdvisor tokenUsage;
    private final LlmGateway llm;
    private final AiGuard guard;
    private final AiProperties properties;
    private final JdbcClient jdbc;

    public BudgetAnalystAgent(ChatClient chatClient, PromptLoader prompts, ErpDataService erp,
                              PendingActionService pendingActions, TokenUsageAdvisor tokenUsage, LlmGateway llm,
                              AiGuard guard, AiProperties properties, JdbcClient jdbc) {
        this.chatClient = chatClient;
        this.prompts = prompts;
        this.erp = erp;
        this.pendingActions = pendingActions;
        this.tokenUsage = tokenUsage;
        this.llm = llm;
        this.guard = guard;
        this.properties = properties;
        this.jdbc = jdbc;
    }

    public AgentResult run(String username, String token, int fiscalYear) {
        guard.check(username);
        String runId = UUID.randomUUID().toString();
        String input = "Analyze budgets for fiscal year " + fiscalYear;
        BudgetAnalystTools tools = new BudgetAnalystTools(erp, pendingActions, token, username,
                properties.agent().maxSteps());

        String status = "DONE";
        String report;
        try {
            report = llm.call(() -> chatClient.prompt()
                    .system(prompts.text("budget-analyst"))
                    .user(input)
                    .advisors(tokenUsage)
                    .advisors(a -> a.param("username", username))
                    .tools(tools)
                    .call()
                    .content());
            if (tools.hitLimit()) {
                status = "STEP_LIMIT";
            }
        } catch (LlmUnavailableException e) {
            log.warn("Agent run {} failed: {}", runId, e.getMessage());
            status = "FAILED";
            report = "The agent could not finish because the AI provider was not available.";
        }

        jdbc.sql("""
                INSERT INTO agent_run (id, username, agent_name, input_text, steps, status, result_text)
                VALUES (CAST(:id AS uuid), :username, 'budget-analyst', :input, :steps, :status, :result)
                """)
                .param("id", runId)
                .param("username", username)
                .param("input", input)
                .param("steps", tools.stepsUsed())
                .param("status", status)
                .param("result", report)
                .update();

        return new AgentResult(runId, status, tools.stepsUsed(), report);
    }
}
