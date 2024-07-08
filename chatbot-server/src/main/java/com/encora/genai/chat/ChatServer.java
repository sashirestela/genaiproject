package com.encora.genai.chat;

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
import com.encora.genai.support.Quote;

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
                + "Para responder usa solo la actual conversación y el contexto agregado a cada consulta. "
                + "Responde las consultas y agrega breves detalles. Responde con tono didáctico y amable. "
                + "No busques otras fuentes de información más que el contexto de cada consulta y la "
                + "propia conversación. Si no hay contexto disponible aquí, dí que lamentas no haber "
                + "encontrado información para responder. Responde en el idioma en que te consulten. "
                + "El contexto lo recibirás en bloques identificados con un ROWID el cuál debes usarlo "
                + "como cita por cada bloque de contexto que uses para responder. La cita tendrá la forma "
                + "[ROWID] y la insertarás, si existe, en el punto de tu respuesta en donde la usaste.";
        SystemMessage message = SystemMessage.of(prompt);
        LOGGER.debug("New chat was required.");
        return message;
    }

    public Stream<String> askQuestionAndGetResponse(List<ChatMessage> messages, String question) {
        String rewrittenQuestion = rewriteQuestion(messages, question);
        List<Double> questionEmbedding = GenerativeAI.createEmbedding(rewrittenQuestion);
        List<FragmentResult> fragments = Database.selectFragments(questionEmbedding, MATCH_THRESHOLD, MATCH_COUNT);
        String prompt = ""
                + "Toma en cuenta la siguiente información como contexto:\n\n"
                + showContextIfExist(fragments) + "\n\n"
                + "Si existe contexto, úsalo para responder la siguiente consulta:\n\n"
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
            return Stream.concat(response, Stream.of(Quote.serializeForQuotes(fragments)));
        }
    }

    private String rewriteQuestion(List<ChatMessage> messages, String question) {
        String prompt = ""
                + "El usuario y la IA están teniendo una conversación acerca de un documento. "
                + "Aquí está la transcripción más reciente de la conversación:\n\n"
                + chatHistory(messages) + "\n\n"
                + "Reescribe la siguiente consulta del usuario, usando la conversación como "
                + "contexto para aclarar principalmente el sujeto y el objeto de la consulta, "
                + "y manteniendo el sentido original de la consulta:\n\n"
                + question;
        Chat chat = GenerativeAI.executeChatCompletion(Arrays.asList(UserMessage.of(prompt)));
        LOGGER.debug("The question was rewriten as: {}", chat.firstContent());
        return chat.firstContent();
    }

    private String showContextIfExist(List<FragmentResult> fragments) {
        String context = fragments.stream()
                .map(fr -> fr.getRowid() + "\n" + fr.getContent())
                .collect(Collectors.joining("\n\n"));
        return context.isEmpty() ? "(No existe contexto)" : context;
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
