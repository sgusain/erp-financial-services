package com.erp.ai.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Loads prompt files from src/main/resources/prompts/<name>.st. Prompts live in files, not in Java. */
@Component
public class PromptLoader {

    /** The raw prompt text (no variables filled in). */
    public String text(String name) {
        try (InputStream in = new ClassPathResource("prompts/" + name + ".st").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt file not found: " + name, e);
        }
    }

    /** The prompt with {placeholders} replaced by the given values. */
    public String render(String name, Map<String, Object> variables) {
        return new PromptTemplate(text(name)).render(variables);
    }
}
