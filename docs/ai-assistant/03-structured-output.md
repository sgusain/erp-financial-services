# 03. Structured output (Course 1 part 4)

## Goal
Get Java objects from the model instead of free text.

## Steps
1. `record TransactionCategory(String category, double confidence, String reason)` in `dto/`. Allowed categories come from an enum.
2. `CategorizationService.categorize(description, amount)` uses `BeanOutputConverter` (or the `.entity(...)` call on ChatClient) and returns the record.
3. `POST /api/ai/categorize` returns the record. `POST /api/ai/tags` returns `List<String>` using `ListOutputConverter`. `POST /api/ai/summary-fields` returns `Map<String,Object>` using `MapOutputConverter`.
4. Handle invalid model output: retry once, then return a 502 with the standard `ErrorResponse`. Never return raw model text on failure.
5. Validate the result: category must be in the enum, confidence between 0 and 1. If not, treat it as invalid output.

## Done when
- Three endpoints work and return typed JSON.
- Unit tests with a stub model cover: valid output, malformed JSON (retry then 502), unknown category.

## Interview notes
- "The model returns text but my code needs objects. BeanOutputConverter adds the JSON format to the prompt and converts the answer into a Java class. I would validate the result and retry once on bad output."
