package com.erp.ai.advisor;

import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

/**
 * Hides emails, card numbers and phone numbers in the user's message BEFORE it goes to the model.
 * It runs first, so the masked text is also what gets saved in chat memory.
 */
@Component
public class PiiMaskingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d ()-]{8,}\\d");

    @Override
    public String getName() {
        return "PiiMaskingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(mask(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(mask(request));
    }

    private ChatClientRequest mask(ChatClientRequest request) {
        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(user -> user.mutate().text(maskText(user.getText())).build()))
                .build();
    }

    /** Public so it can be unit tested without a model. */
    public static String maskText(String text) {
        if (text == null) {
            return null;
        }
        String result = EMAIL.matcher(text).replaceAll("[EMAIL]");
        result = CARD.matcher(result).replaceAll("[CARD]");
        return PHONE.matcher(result).replaceAll("[PHONE]");
    }
}
