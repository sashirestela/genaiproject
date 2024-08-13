package com.encora.genai.service;

import static com.encora.genai.support.Commons.MATCH_COUNT;
import static com.encora.genai.support.Commons.MATCH_THRESHOLD;
import static com.encora.genai.support.Commons.PROMPT_NO_CONTEXT;
import static com.encora.genai.support.Commons.PROMPT_PREV_STEP_SYSTEM;
import static com.encora.genai.support.Commons.PROMPT_MAIN_STEP_SYSTEM;
import static com.encora.genai.support.Commons.TEMPLATE_CONTEXT_FRAGMENT;
import static com.encora.genai.support.Commons.TEMPLATE_ENHANCED_QUESTION;
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
import com.encora.genai.functions.GetContentByArticles;
import com.encora.genai.support.Database;
import com.encora.genai.support.GenerativeAI;
import com.encora.genai.support.Quote;

import io.github.sashirestela.openai.common.function.FunctionCall;
import io.github.sashirestela.openai.common.function.FunctionDef;
import io.github.sashirestela.openai.common.function.FunctionExecutor;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private static ChatService chatService = null;
    private static FunctionExecutor executor = null;

    private ChatService() {
    }

    public static ChatService one() {
        if (chatService == null) {
            chatService = new ChatService();
            executor = new FunctionExecutor();
            executor.enrollFunction(FunctionDef.builder()
                    .name(GetContentByArticles.class.getSimpleName())
                    .functionalClass(GetContentByArticles.class)
                    .description(GetContentByArticles.DESCRIPTION)
                    .build());
        }
        return chatService;
    }

    public SystemMessage getSystemMessage() {
        return SystemMessage.of(PROMPT_MAIN_STEP_SYSTEM);
    }

    @SuppressWarnings("unchecked")
    public Stream<String> askQuestionAndGetResponse(List<ChatMessage> messages, String question) {
        String rewrittenQuestion = null;
        List<FragmentResult> fragments = null;
        Chat previousStep = runPreviousStep(messages, question, executor);
        
        if (getTypeResponse(previousStep) == TypeResponse.QUESTION_WAS_REWRITTEN) {
            rewrittenQuestion = previousStep.firstContent();
            List<Double> questionEmbedding = GenerativeAI.createEmbedding(rewrittenQuestion);
            fragments = Database.selectFragments(questionEmbedding, MATCH_THRESHOLD, MATCH_COUNT);
        } else {
            rewrittenQuestion = question;
            FunctionCall functionCall = previousStep.firstMessage().getToolCalls().get(0).getFunction();
            fragments = (List<FragmentResult>) executor.execute(functionCall);
        }

        String enhancedQuestion = replacePlaceholders(TEMPLATE_ENHANCED_QUESTION, Map.of(
                "contextForQuestion", generateContext(fragments),
                "rewrittenQuestion", rewrittenQuestion));
        List<ChatMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(UserMessage.of(enhancedQuestion));
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

    private Chat runPreviousStep(List<ChatMessage> messages, String question, FunctionExecutor executor) {
        String systemPrompt = replacePlaceholders(PROMPT_PREV_STEP_SYSTEM,
                Map.of("chatHistory", chatHistory(messages)));
        List<ChatMessage> prevStepMessages = Arrays.asList(SystemMessage.of(systemPrompt), UserMessage.of(question));
        Chat chat = GenerativeAI.executeChatCompletion(prevStepMessages, executor.getToolFunctions());
        LOGGER.debug("The previous step was ran: a) the question was rewritten, or b) articles were detected.");
        return chat;
    }

    private String generateContext(List<FragmentResult> fragments) {
        String context = fragments.stream()
                .map(fr -> replacePlaceholders(TEMPLATE_CONTEXT_FRAGMENT,
                        Map.of("id", fr.getRowid().toString(), "contenido", fr.getContent())))
                .collect(Collectors.joining("\n"));
        return context.isEmpty() ? PROMPT_NO_CONTEXT : context;
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

    private TypeResponse getTypeResponse(Chat response) {
        if (response.getChoices().get(0).getFinishReason().equals("tool_calls")) {
            return TypeResponse.ARTICLES_WERE_DETECTED;
        } else {
            return TypeResponse.QUESTION_WAS_REWRITTEN;
        }
    }

    static enum TypeResponse {
        QUESTION_WAS_REWRITTEN,
        ARTICLES_WERE_DETECTED;
    }

}
