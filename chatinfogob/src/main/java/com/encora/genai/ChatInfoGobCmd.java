package com.encora.genai;

import static com.encora.genai.support.Commons.EMPTY;
import static com.encora.genai.support.Commons.getUserInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.service.ChatService;
import com.encora.genai.service.UploadService;
import com.encora.genai.support.Quote;

import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class ChatInfoGobCmd {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatInfoGobCmd.class);

    public static void main(String[] args) throws IOException {
        String command = args != null && args.length > 0 ? args[0] : "chat";

        if ("upload".equalsIgnoreCase(command)) {

            String fullFileName = getUserInput("Escriba la ruta del archivo PDF: ", EMPTY);
            UploadService.one().processPdf(fullFileName);

        } else if ("chat".equalsIgnoreCase(command)) {

            List<ChatMessage> messages = new ArrayList<>();
            SystemMessage message = ChatService.one().getSystemMessage();
            messages.add(message);
            LOGGER.debug("New chat was started.");
            String userQuestion;
            while (!(userQuestion = getUserInput("CONSULTA: ", EMPTY)).isEmpty()) {
                Stream<String> chatResponse = ChatService.one().askQuestionAndGetResponse(messages, userQuestion);
                System.out.print("RESPUESTA: ");
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
                    Map<String, String> matchedQuotes = Quote.matchQuotes(
                            response.toString(), serializedQuotes.toString());
                    matchedQuotes.forEach(
                            (rowid, reference) -> System.out.println("[" + rowid + "] " + reference));
                }
                System.out.print("\n\n");
                messages.add(UserMessage.of(userQuestion));
                messages.add(AssistantMessage.of(response.toString()));

            }
        }
    }
}
