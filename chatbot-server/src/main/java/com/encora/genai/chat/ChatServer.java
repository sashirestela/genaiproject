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
    private static final Integer MATCH_COUNT = 4;

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
                + "Para responder usa la conversación y principalmente el contexto agregado a cada consulta. "
                + "Responde las consultas y agrega breves detalles. Responde con tono didáctico y amable. "
                + "No busques otras fuentes de información más que el contexto de cada consulta y la "
                + "propia conversación. Si no hay información disponible aquí, dí que lamentas no haber "
                + "encontrado información para responder. Responde en el idioma en que te consulten.\n\n"
                + "Las consultas las recibirás de la siguiente forma:\n\n"
                + "   Toma en cuenta la siguiente información:\n\n"
                + "   1\n"
                + "   Los perros ladran y son animales domésticos.\n\n"
                + "   2\n"
                + "   Los gatos maullan y son animales domésticos.\n\n"
                + "   3\n"
                + "   Los caballos relinchan, y no son animales domésticos.\n\n"
                + "   Si existe información, úsala para responder la siguiente consulta:\n\n"
                + "   qué aninales ladran o maullan?\n\n"
                + "Si usas algunos de los bloque para responder a la consulta, debes agregar el número "
                + "encima de cada bloque al final de tu respuesta, de la siguiente forma:\n\n"
                + "   El animal que ladra es el perro[1]. El animal que maulla es el gato[2]. Ambos animales "
                + "   son domésticos.'";
        SystemMessage message = SystemMessage.of(prompt);
        LOGGER.debug("New chat was required.");
        return message;
    }

    public Stream<String> askQuestionAndGetResponse(List<ChatMessage> messages, String question) {
        String rewrittenQuestion = rewriteQuestion(messages, question);
        List<Double> questionEmbedding = GenerativeAI.createEmbedding(rewrittenQuestion);
        List<FragmentResult> fragments = Database.selectFragments(questionEmbedding, MATCH_THRESHOLD, MATCH_COUNT);
        String prompt = ""
                + "Toma en cuenta la siguiente información:\n\n"
                + showContextIfExist(fragments) + "\n\n"
                + "Si existe información, úsala para responder la siguiente consulta:\n\n"
                + rewrittenQuestion;
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
        String prompt = ""
                + "El usuario está haciendo preguntas sobre la Constitución Política del Perú. "
                + "Aquí está la transcripción cronológica de todas sus preguntas:\n\n"
                + chatHistory(messages) + "\n\n"
                + "Reescribe la siguiente consulta, usando el historial de preguntas anteriores "
                + "como contexto para aclarar principalmente el sujeto y el objeto de la consulta."
                + "Debes mantener el sentido original de la consulta:\n\n"
                + question;
        Chat chat = GenerativeAI.executeChatCompletion(Arrays.asList(UserMessage.of(prompt)));
        LOGGER.debug("The question was rewriten as: {}", chat.firstContent());
        return chat.firstContent();
    }

    private String showContextIfExist(List<FragmentResult> fragments) {
        String context = fragments.stream()
                .map(fr -> fr.getRowid() + "\n" + fr.getContent())
                .collect(Collectors.joining("\n\n"));
        return context.isEmpty() ? "(No existe información)" : context;
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
