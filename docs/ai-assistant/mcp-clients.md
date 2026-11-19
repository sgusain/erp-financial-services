# Connect AI clients to the ERP MCP server (spec 10)

This works after spec 10 is implemented. The ERP MCP server exposes read-only tools: account balance, budget status, policy search.

## Get a token
Log in through the gateway and copy the JWT:

```
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"<user>","password":"<password>"}'
```

Put the token in an environment variable, never in a file you commit:

```
export ERP_MCP_TOKEN="<paste token here>"
```

## Claude Code
Add the server to your local MCP config (see `.mcp.json.example`, entry `erp-ai-assistant`). The URL and path depend on how spec 10 configures the MCP endpoint; check `application.yml` in `ai-assistant-service`.

## VS Code
Add the same server in the MCP settings with the `Authorization: Bearer ${ERP_MCP_TOKEN}` header.

## Check
- Listing tools shows only read-only tools.
- A call without a token fails with 401.
- Tokens expire after 15 minutes (the ERP access token lifetime), so refresh when calls start failing.
