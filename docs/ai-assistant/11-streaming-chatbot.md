# 11. Streaming chatbot (Course 1 part 13)

## Goal
Stream answers token by token and provide a simple chat page.

## Steps
1. `GET`/`POST /api/ai/chat/stream` returns `text/event-stream` using `ChatClient.stream()` returning a `Flux`. Memory, advisors and tools still apply. On error send an `event: error` message and close.
2. Client disconnect cancels the model call (verify no leak).
3. Add a plain static page `src/main/resources/static/chat.html` (HTML and JavaScript, no framework) that logs in with a JWT field, sends a message, and appends tokens as they arrive. Include a Stop button and show tool-call and confirm prompts for pending actions (spec 08).
4. CORS or gateway config so the page works through the gateway.
5. MVC vs reactive: keep the service on Spring MVC. Use `Flux` only for the stream endpoint (needs the reactor dependency that Spring AI already brings).

## Done when
- Text appears word by word in the browser.
- Test with `WebTestClient` (or MockMvc async) reads multiple SSE chunks from a stub streaming model.
- Cancel works.

## Interview notes
- "Waiting for the full answer feels slow. I would stream tokens with server-sent events from a Flux so the user sees text immediately, and cancel the call when the user stops."
