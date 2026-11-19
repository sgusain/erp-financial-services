# 12. Production hardening

## Goal
Make LLM calls safe to run: resilient, limited, observable, cost-controlled.

## Steps
1. Resilience4j (already used in the repo): timeout, circuit breaker and retry around model calls. Treat 429 and 5xx as retryable, 4xx as not.
2. Fallback model: when the main provider circuit is open, fall back to Ollama (or return a friendly "assistant unavailable" error if Ollama is down). Log the fallback.
3. Per-user rate limit (Redis, already available): for example 20 requests per minute per user. Return 429 with `Retry-After`.
4. Cost control: daily token budget per user and global (config). When exceeded return 429 with a clear message. Metrics from spec 04 feed this.
5. Input limits: max message length, max tool results size, max conversation window.
6. Observability: Micrometer timers `ai.call.duration`, counters for errors and fallbacks, correlation id in logs (existing ELK pipeline). Add a Kibana-friendly JSON field for provider and model.
7. Secrets: keys only from env. Add `AI_*` and key variables to `.env.example` (empty values) and compose files. Do not commit real values.
8. Add k8s manifests for ai-assistant-service in `k8s/` following the existing pattern (deployment, service, hpa, secret reference). Dry-run validate like the others.

## Done when
- Test: provider stub fails 5 times, circuit opens, fallback answers.
- Test: 21st request in a minute gets 429.
- Test: token budget exceeded gets 429.
- `kubectl apply --dry-run=client` passes for the new manifests.

## Interview notes
- "LLM calls are slow, cost money and can fail. I would add timeouts, a circuit breaker, per-user limits, a token budget, metrics and a fallback model."
