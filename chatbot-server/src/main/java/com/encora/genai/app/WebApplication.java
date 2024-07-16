package com.encora.genai.app;

import static com.encora.genai.support.Commons.HTTP_PORT;
import static com.encora.genai.support.Commons.MIMETYPE_PDF;
import static com.encora.genai.support.Commons.THREAD_POOL_SIZE;
import static com.encora.genai.support.Commons.UPLOAD_FOLDER;
import static com.encora.genai.support.Commons.inputStreamToString;
import static com.encora.genai.support.Commons.replacePlaceholders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.RequestContext;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.encora.genai.chat.ChatServer;
import com.encora.genai.ingest.LoadPdfDocument;
import com.encora.genai.support.Quote;
import com.encora.genai.transfer.ChatClientRequest;
import com.encora.genai.util.HttpHandlerRequestContext;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.github.sashirestela.cleverclient.util.JsonUtil;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

public class WebApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplication.class);

    private HttpServer httpServer;

    public WebApplication() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        httpServer.createContext("/chat", new ChatHandler());
        httpServer.createContext("/upload", new UploadHandler());
    }

    public void start() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        httpServer.setExecutor(threadPoolExecutor);
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }

    public static class UploadHandler implements HttpHandler {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsAndCheckIfOptionsWasCalled(exchange)) {
                return;
            }
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("POST")) {
                try {
                    RequestContext context = new HttpHandlerRequestContext(exchange);
                    DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
                    JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);

                    List<FileItem> items = upload.parseRequest(context);
                    for (FileItem item : items) {
                        if (!item.isFormField() && MIMETYPE_PDF.equals(item.getContentType())) {
                            String fileName = item.getName();
                            String fullFileName = UPLOAD_FOLDER + fileName;
                            File file = new File(fullFileName);
                            try (InputStream input = item.getInputStream();
                                    FileOutputStream output = new FileOutputStream(file)) {
                                IOUtils.copy(input, output);
                            }
                            LoadPdfDocument loader = new LoadPdfDocument();
                            loader.process(fullFileName);
                            String response = "File uploaded successfully: " + fileName;
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes().length);
                            exchange.getResponseBody().write(response.getBytes());
                            exchange.close();
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String response = "Error processing upload: " + e.getMessage();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                }
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }
        }
    }

    public static class ChatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsAndCheckIfOptionsWasCalled(exchange)) {
                return;
            }
            String method = exchange.getRequestMethod();
            InputStream inputStream = exchange.getRequestBody();
            if (method.equalsIgnoreCase("POST")) {
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "text/event-stream");
                headers.set("Cache-Control", "no-cache");
                headers.set("Connection", "keep-alive");

                String body = inputStreamToString(inputStream);
                ChatClientRequest chatClientRequest = JsonUtil.jsonToObject(body, ChatClientRequest.class);
                String question = chatClientRequest.getQuestion();
                List<ChatMessage> messages = new LinkedList<>();
                messages.add(ChatServer.one().getSystemMessage());
                if (chatClientRequest.getMessages() == null || chatClientRequest.getMessages().isEmpty()) {
                    LOGGER.debug("New chat was started.");
                } else {
                    messages.addAll(chatClientRequest.getChatMessages());
                }

                Stream<String> chatResponse = ChatServer.one().askQuestionAndGetResponse(messages, question);

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                OutputStream outputStream = exchange.getResponseBody();
                StringBuilder response = new StringBuilder();
                StringBuilder serializedQuotes = new StringBuilder();
                chatResponse.forEach(token -> {
                    if (Quote.isQuote(token)) {
                        serializedQuotes.append(token);
                    } else {
                        response.append(token);
                        write(outputStream, "delta", token, true);
                    }
                });
                if (!serializedQuotes.isEmpty()) {
                    Map<String, String> matchedQuotes = Quote.matchQuotes(
                            response.toString(), serializedQuotes.toString());
                    matchedQuotes.forEach((rowid, reference) -> {
                        write(outputStream, "quote", "[" + rowid + "] " + reference, false);
                    });
                }
                write(outputStream, "chatmessage", JsonUtil.objectToJson(UserMessage.of(question)), false);
                write(outputStream, "chatmessage", JsonUtil.objectToJson(AssistantMessage.of(response.toString())),
                        false);
                write(outputStream, "done", "[DONE]", false);
            } else {
                writeError(exchange, "Method Not Allowed");
            }
            exchange.close();
        }

        private void write(OutputStream outputStream, String event, String data, boolean isForHtml) {
            String newData = isForHtml ? data.replaceAll("\\r\\n|\\n|\\r", "<br>") : data;
            String sseTemplate = "event: {event}\ndata: {data}\n\n";
            String sseMessage = replacePlaceholders(sseTemplate, Map.of("event", event, "data", newData));
            try {
                outputStream.write(sseMessage.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeError(HttpExchange exchange, String errorMessage) throws IOException {
            byte[] responseError = errorMessage.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, responseError.length);
            exchange.getResponseBody().write(responseError);
        }

    }

    private static boolean handleCorsAndCheckIfOptionsWasCalled(HttpExchange exchange) throws IOException {
        boolean optionsWasCalled = false;
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
            optionsWasCalled = true;
        }
        return optionsWasCalled;
    }

    public static void main(String[] args) throws IOException {
        WebApplication webApp = new WebApplication();
        webApp.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                webApp.stop();
                System.out.println("\nThe application was stopped!");
            }
        });
    }

}
