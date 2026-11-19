# 16. MCP for the developer workflow (Course 2 section 9)

## Goal
Use MCP servers in your own Claude Code and VS Code workflow, and understand the security side.

## Steps
1. **Context7** (live library docs): add it to your Claude Code and VS Code MCP config. Test: ask for the Spring AI MCP server starter setup and check the answer uses current docs, not old ones.
2. **GitHub MCP**: add it with a fine-grained token limited to this repo (contents and pull requests only, short expiry). Use it to create a branch and commit a small docs change. Review the diff before it pushes.
3. **Config files**: project-level `.mcp.json` is committed only with placeholders and `${ENV_VAR}` references. Real tokens come from your environment. Add the real local file to `.gitignore` if it holds anything private.
4. Write `docs/ai-assistant/mcp-security.md` (one page, your own words): least privilege, read-only by default, separate tokens per server, tool output is untrusted (prompt injection), review before approving tool calls, rotate tokens.
5. Run one prompt injection experiment: put a hostile instruction inside a GitHub issue or a file and see whether the agent follows it. Record what happened and which setting stopped it.

## Done when
- Both servers work in Claude Code.
- No token is committed anywhere (run a secret scan such as `git grep` for common key prefixes).
- Security note and experiment result are in the docs folder.

## Interview notes
- "Context7 gives the agent current library docs so it does not use old versions from its training data. For MCP I use least privilege and treat tool output as untrusted."
