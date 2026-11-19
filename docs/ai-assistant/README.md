# ERP AI Assistant: Spec Set

Learning project that covers EmbarkX "Build AI-Driven Apps with Java" (Course 1) and "GenAI & AI Agents for Java Spring Boot Devs" (Course 2) inside this repo.

New module: `ai-assistant-service` (port 8086). Read `00-overview.md` first.

## How to run a spec with Claude Code

1. Start a fresh Claude Code session per spec (keeps context small).
2. Prompt: `Read CLAUDE.md and docs/ai-assistant/00-overview.md, then implement docs/ai-assistant/NN-name.md. Stop when every item under "Done when" passes. Do not touch other services unless the spec says so.`
3. Review the diff yourself, run the tests, then commit one spec per commit.
4. Read the "Interview notes" at the bottom of the spec and say the answer out loud.

## Course 1 (build AI features in Java)

| Spec | Topic | Course 1 part |
|---|---|---|
| 01-foundations-and-providers | ChatClient, provider switching | 2, 3 |
| 02-prompt-engineering | Templates, few-shot, chaining | 4 |
| 03-structured-output | BeanOutputConverter and friends | 4 |
| 04-advisors | Built-in and custom advisors | 5 |
| 05-chat-memory | Conversation memory per user | 6, 7 |
| 06-embeddings-pgvector | Embeddings and vector search | 8 |
| 07-rag | Retrieval Augmented Generation | 9 |
| 08-tool-calling | Tools calling ERP services | 10 |
| 09-agents | Budget Analyst agent | 11 |
| 10-mcp | MCP server and client | 12 |
| 11-streaming-chatbot | SSE streaming and chat page | 13 |
| 12-production-hardening | Resilience, limits, cost, fallback | extra |

## Course 2 (use AI agents as a developer)

| Spec | Topic | Course 2 sections |
|---|---|---|
| 13-claude-code-workflow | CLAUDE.md, skills, sub-agents, refactor | 4, 8 |
| 14-pr-review-agent-ci | AI code review in GitHub Actions | 11, 12, 13 |
| 15-slow-query-finder | Database exploration agent | 14 |
| 16-mcp-dev-workflow | Context7, GitHub MCP, security | 9 |
| 17-n8n-automation | n8n GitHub digest workflow | 15 |

Sections 5, 6, 7 (JetBrains Junie/Air/ACP, Google Antigravity, GitHub Copilot) are tool tours with nothing to build. Spend 1 to 2 hours total trying them.

## Extra (in neither course)

| Spec | Topic |
|---|---|
| 18-langchain4j-langgraph4j | Rebuild the agent in LangChain4j, then LangGraph4j |

## Interview wording rule

Say "I would..." until you have built and verified the spec. After that, say "In my ERP assistant, I built...".
