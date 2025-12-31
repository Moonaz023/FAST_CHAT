package com.example.fast_chat.repository.conversation;

import com.example.fast_chat.model.conversation.ConversationMessageAi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ConversationMessageAiRepository extends
    ReactiveMongoRepository<ConversationMessageAi, String> {

  Flux<ConversationMessageAi> findByConversationIdOrderByCreatedAtAsc(String conversationId);

  Mono<Long> countByConversationId(String conversationId);

  Mono<Void> deleteByConversationId(String conversationId);

  Flux<ConversationMessageAi> findByConversationId(String conversationId, Pageable pageable);

  Flux<ConversationMessageAi> findTop10ByConversationIdOrderByCreatedAtDesc(String conversationId);
}