package com.encora.genai.chat;

import static com.encora.genai.support.Commons.MATCH_COUNT;
import static com.encora.genai.support.Commons.MATCH_THRESHOLD;
import static com.encora.genai.support.Commons.PROMPT_ENHANCED_QUESTION;
import static com.encora.genai.support.Commons.PROMPT_REWRITE_QUESTION;
import static com.encora.genai.support.Commons.PROMPT_SYSTEM;
import static com.encora.genai.support.Commons.PROMPT_WITHOUT_INFORMATION;
import static com.encora.genai.support.Commons.replacePlaceholders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.FragmentResult;
import com.encora.genai.support.Database;
import com.encora.genai.support.GenerativeAI;
import com.encora.genai.support.Quote;

import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class ChatServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class);

    private static ChatServer chatServer = null;

    private ChatServer() {
    }

    public static ChatServer one() {
        if (chatServer == null) {
            chatServer = new ChatServer();
        }
        return chatServer;
    }

    public SystemMessage getSystemMessage() {
        return SystemMessage.of(PROMPT_SYSTEM);
    }

    public Stream<String> askQuestionAndGetResponse(List<ChatMessage> messages, String question) {
        String rewrittenQuestion = rewriteQuestion(messages, question);
        List<Double> questionEmbedding = GenerativeAI.createEmbedding(rewrittenQuestion);
        List<FragmentResult> fragments = Database.selectFragments(questionEmbedding, MATCH_THRESHOLD, MATCH_COUNT);
        String prompt = replacePlaceholders(PROMPT_ENHANCED_QUESTION,
                Map.of("contextForQuestion", showContextIfExist(fragments), "rewrittenQuestion", rewrittenQuestion));
        List<ChatMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(UserMessage.of(prompt));
        Stream<Chat> chatStream = GenerativeAI.executeSreamChatCompletion(updatedMessages);
        Stream<String> response = chatStream
                .filter(chat -> !chat.getChoices().isEmpty() && chat.firstContent() != null)
                .map(Chat::firstContent);
        LOGGER.debug("A response was received.");
        if (fragments.isEmpty()) {
            return response;
        } else {
            return Stream.concat(response, Stream.of(Quote.serializeForQuotes(fragments)));
        }
    }

    private String rewriteQuestion(List<ChatMessage> messages, String question) {
        String prompt = replacePlaceholders(PROMPT_REWRITE_QUESTION,
                Map.of("chatHistory", chatHistory(messages), "originalQuestion", question));
        Chat chat = GenerativeAI.executeChatCompletion(Arrays.asList(UserMessage.of(prompt)));
        LOGGER.debug("The question was rewriten as: {}", chat.firstContent());
        return chat.firstContent();
    }

    private String showContextIfExist(List<FragmentResult> fragments) {
        String context = fragments.stream()
                .map(fr -> fr.getRowid() + "\n" + fr.getContent())
                .collect(Collectors.joining("\n\n"));
        return context.isEmpty() ? PROMPT_WITHOUT_INFORMATION : context;
    }

    private String chatHistory(List<ChatMessage> messages) {
        return messages.stream()
                .filter(msg -> (msg instanceof UserMessage))
                .map(msg -> {
                    String chatEntry = msg.getRole().name() + ": ";
                    if (msg instanceof AssistantMessage) {
                        chatEntry += ((AssistantMessage) msg).getContent();
                    } else if (msg instanceof UserMessage) {
                        chatEntry += (String) ((UserMessage) msg).getContent();
                    }
                    return chatEntry;
                })
                .collect(Collectors.joining("\n"));
    }

}
