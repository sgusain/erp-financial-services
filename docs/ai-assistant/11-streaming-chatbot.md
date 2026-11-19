# 11. Streaming chatbot (Course 1 part 13)

## Goal
Stream answers token by token and provide a simple chat page.

## Steps
1. `GET`/`POST /api/ai/chat/stream` returns `text/event-stream` using `ChatClient.stream()` returning a `Flux`. Memory, advisors and tools still apply. On error send an `event: error` message and close.
2. Client disconnect cancels the model call (verify no leak).
3. Extend the existing `static/chat.html` from spec 01 (still plain HTML and JavaScript, no framework) so it appends tokens as they arrive. Add a Stop button, a "new chat" button and chat history list (spec 05), and show tool-call and confirm prompts for pending actions (spec 08).
4. CORS or gateway config so the page works through the gateway.
5. MVC vs reactive: keep the service on Spring MVC. Use `Flux` only for the stream endpoint (needs the reactor dependency that Spring AI already brings).

## Done when
- Text appears word by word in the browser.
- Test with `WebTestClient` (or MockMvc async) reads multiple SSE chunks from a stub streaming model.
- Cancel works.

## Interview notes
- "Waiting for the full answer feels slow. I would stream tokens with server-sent events from a Flux so the user sees text immediately, and cancel the call when the user stops."
