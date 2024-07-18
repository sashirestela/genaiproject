package com.encora.genai.transfer;

import java.util.List;

import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.AssistantMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatClientRequest {

    private String question;
    private List<Message> messages;

    public List<ChatMessage> getChatMessages() {
        return messages.stream().map(msg -> msg.convertToChatMessage()).toList();
    }

    @Data
    @NoArgsConstructor
    public static class Message {

        private String role;
        private String content;

        public ChatMessage convertToChatMessage() {
            if (role.equals("system")) {
                return SystemMessage.of(content);
            } else if (role.equals("user")) {
                return UserMessage.of(content);
            } else if (role.equals("assistant")) {
                return AssistantMessage.of(content);
            } else {
                return null;
            }
        }

    }
}
