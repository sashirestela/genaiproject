package com.encora.genai.chat;

import static com.encora.genai.support.Commons.EMPTY;
import static com.encora.genai.support.Commons.getUserInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.support.Quote;

import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class ChatClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatClient.class);

    private static final String USER_INPUT_MESSAGE = "CONSULTA: ";
    private static final String ASSISTANT_MESSAGE = "RESPUESTA: ";

    private List<ChatMessage> messages;

    public ChatClient() {
        messages = new ArrayList<>();
    }

    public void startChat() {
        SystemMessage message = ChatServer.one().getSystemMessage();
        messages.add(message);
        LOGGER.debug("New chat was started.");

        String userQuestion;

        while (!(userQuestion = getUserInput(USER_INPUT_MESSAGE, EMPTY)).isEmpty()) {
            Stream<String> chatResponse = ChatServer.one().askQuestionAndGetResponse(messages, userQuestion);
            System.out.print(ASSISTANT_MESSAGE);
            StringBuilder response = new StringBuilder();
            StringBuilder serializedQuotes = new StringBuilder();
            chatResponse.forEach(str -> {
                if (Quote.isQuote(str)) {
                    serializedQuotes.append(str);
                } else {
                    response.append(str);
                    System.out.print(str);
                }
            });
            if (!serializedQuotes.isEmpty()) {
                System.out.print("\n\n");
                Map<String, String> matchedQuotes = Quote.matchQuotes(response.toString(), serializedQuotes.toString());
                matchedQuotes.forEach((rowid, reference) -> System.out.println("[" + rowid + "] " + reference));
            }
            System.out.print("\n\n");
            messages.add(UserMessage.of(userQuestion));
            messages.add(AssistantMessage.of(response.toString()));
        }
    }

}
