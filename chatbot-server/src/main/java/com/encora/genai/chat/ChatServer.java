package com.encora.genai.chat;

import static com.encora.genai.support.Commons.MARK_FOR_REFERENCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.data.FragmentResult;
import com.encora.genai.support.Database;
import com.encora.genai.support.GenerativeAI;

import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class ChatServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class);

    private static final Double MATCH_THRESHOLD = 0.45;
    private static final Integer MATCH_COUNT = 3;

    private static ChatServer chatServer = null;

    private ChatServer() {
    }

    public static ChatServer one() {
        if (chatServer == null) {
            chatServer = new ChatServer();
        }
        return chatServer;
    }

    public SystemMessage getMessageToStartChat() {
        String prompt = ""
                + "Para responder consultas usa solo el historial de mensajes de la conversación y el "
                + "contexto que agregue en cada consulta. Responde a la consulta con cierto detalle. "
                + "No inventes respuestas. Si no hay información para responder, dí que no hallaste "
                + "información para responder a la consulta. Responde en el idioma en que consulten.";
        SystemMessage message = SystemMessage.of(prompt);
        LOGGER.debug("New chat was required.");
        return message;
    }

    public Stream<String> askQuestionAndGetResponse(List<ChatMessage> messages, String question) {
        String rewrittenQuestion = messages.size() > 1 ? rewriteQuestion(messages, question) : question;
        List<Double> questionEmbedding = GenerativeAI.createEmbedding(rewrittenQuestion);
        List<FragmentResult> fragments = Database.selectFragments(questionEmbedding, MATCH_THRESHOLD, MATCH_COUNT);
        String prompt = ""
                + "Toma en cuenta la siguiente información como contexto:\n\n"
                + fragments.stream().map(fr -> fr.getReference() + "\n" + fr.getContent())
                        .collect(Collectors.joining("\n\n"))
                + "\n\n"
                + "Si no hay contexto, responde que no hallaste información para responder. "
                + "Si existe un contexto, usalo para responder la siguiente consulta:\n\n"
                + rewrittenQuestion;
        List<ChatMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(UserMessage.of(prompt));
        Stream<Chat> chatStream = GenerativeAI.executeSreamChatCompletion(updatedMessages);
        Stream<String> response = chatStream.filter(chat -> !chat.getChoices().isEmpty() && chat.firstContent() != null)
                .map(Chat::firstContent);
        LOGGER.debug("A response was received.");
        if (fragments.isEmpty()) {
            return response;
        } else {
            return Stream.concat(response,
                    Stream.of(MARK_FOR_REFERENCE + fragments.get(0).getReference()));
        }
    }

    private String rewriteQuestion(List<ChatMessage> messages, String question) {
        String prompt = ""
                + "El usuario y la IA están teniendo una conversación acerca de un documento. "
                + "Aquí está la transcripción más reciente de la conversación:\n\n"
                + chatHistory(messages) + "\n\n"
                + "Reescribe la siguiente consulta del usuario, usando esa conversación como "
                + "contexto, produciendo una consulta que sea más útil para crear un embedding "
                + "para búsqueda semántica:\n\n"
                + question;
        Chat chat = GenerativeAI.executeChatCompletion(Arrays.asList(UserMessage.of(prompt)));
        LOGGER.debug("The question was rewriten as: {}", chat.firstContent());
        return chat.firstContent();
    }

    private String chatHistory(List<ChatMessage> messages) {
        return messages.stream()
                .filter(msg -> !(msg instanceof SystemMessage))
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
