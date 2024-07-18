package com.encora.genai;

import static com.encora.genai.support.Commons.MIMETYPE_PDF;
import static com.encora.genai.support.Commons.UPLOAD_FOLDER;
import static io.jooby.Jooby.runApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.pdfbox.io.IOUtils;

import com.encora.genai.service.ChatService;
import com.encora.genai.service.UploadService;
import com.encora.genai.support.Quote;
import com.encora.genai.transfer.ChatClientRequest;

import io.github.sashirestela.cleverclient.util.JsonUtil;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.jooby.FileUpload;
import io.jooby.ServerSentEmitter;
import io.jooby.StatusCode;
import io.jooby.handler.Cors;
import io.jooby.handler.CorsHandler;
import io.jooby.internal.handler.ServerSentEventHandler;

public class ChatInfoGobWeb {

    public static void main(final String[] args) {
        runApp(args, app -> {
            Cors cors = Cors.from(app.getConfig());
            app.use(new CorsHandler(cors));

            app.post("/upload", ctx -> {
                FileUpload fileUpload = ctx.files().get(0);
                if (fileUpload.getContentType().equals(MIMETYPE_PDF)) {
                    String fullFileName = UPLOAD_FOLDER + fileUpload.getFileName();
                    File file = new File(fullFileName);
                    try (InputStream input = fileUpload.stream();
                            FileOutputStream output = new FileOutputStream(file)) {
                        IOUtils.copy(input, output);
                    }

                    UploadService.one().processPdf(fullFileName);

                    ctx.setResponseCode(StatusCode.OK);
                    return "File uploaded successfully: " + fileUpload.getFileName();
                } else {
                    ctx.setResponseCode(StatusCode.BAD_REQUEST);
                    return "Wrong file type. A pdf file is expected.";
                }
            });

            app.post("/chat", ctx -> {
                String body = ctx.body().value();
                ChatClientRequest chatClientRequest = JsonUtil.jsonToObject(body, ChatClientRequest.class);
                String question = chatClientRequest.getQuestion();
                List<ChatMessage> messages = new LinkedList<>();
                messages.add(ChatService.one().getSystemMessage());
                if (chatClientRequest.getMessages() != null && !chatClientRequest.getMessages().isEmpty()) {
                    messages.addAll(chatClientRequest.getChatMessages());
                }

                Stream<String> chatResponse = ChatService.one().askQuestionAndGetResponse(messages, question);

                StringBuilder response = new StringBuilder();
                StringBuilder serializedQuotes = new StringBuilder();
                ServerSentEventHandler sseHandler = new ServerSentEventHandler(sse -> {
                    chatResponse.forEach(token -> {
                        if (Quote.isQuote(token)) {
                            serializedQuotes.append(token);
                        } else {
                            response.append(token);
                            send(sse, "delta", token, true);
                        }
                    });
                    if (!serializedQuotes.isEmpty()) {
                        Map<String, String> matchedQuotes = Quote.matchQuotes(
                                response.toString(), serializedQuotes.toString());
                        matchedQuotes.forEach(
                                (rowid, reference) -> send(sse, "quote", "[" + rowid + "] " + reference, false));
                    }
                    send(sse, "chatmessage", JsonUtil.objectToJson(UserMessage.of(question)), false);
                    send(sse, "chatmessage", JsonUtil.objectToJson(AssistantMessage.of(response.toString())), false);
                    send(sse, "done", "[DONE]", false);
                    sse.close();
                });
                return sseHandler.apply(ctx);
            });
        });
    }

    private static void send(ServerSentEmitter sse, String event, String data, boolean isForHtml) {
        String newData = isForHtml ? data.replaceAll("\\r\\n|\\n|\\r", "<br>") : data;
        sse.send(" " + event, " " + newData);
    }

}
