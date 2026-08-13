package com.schemebridge.coreservice.ai.repository;

import com.schemebridge.coreservice.ai.model.AiChatConversationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiChatConversationRepository extends MongoRepository<AiChatConversationDocument, String> {

    Optional<AiChatConversationDocument> findByConversationId(String conversationId);

    List<AiChatConversationDocument> findByAuthUserId(String authUserId);

    Optional<AiChatConversationDocument> findByAuthUserIdAndSchemeId(String authUserId, String schemeId);
}
