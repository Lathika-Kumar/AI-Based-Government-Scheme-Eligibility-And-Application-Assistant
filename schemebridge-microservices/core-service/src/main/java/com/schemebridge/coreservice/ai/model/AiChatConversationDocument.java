package com.schemebridge.coreservice.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_chat_conversations")
@CompoundIndex(name = "auth_user_scheme_idx", def = "{'authUserId': 1, 'schemeId': 1}")
public class AiChatConversationDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String conversationId;

    @Indexed
    private String authUserId;

    private String schemeId;

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role; // "user" or "assistant"
        private String content;
        private Instant timestamp;
    }
}
