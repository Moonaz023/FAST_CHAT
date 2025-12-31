package com.example.fast_chat.repository.conversation;

import com.example.fast_chat.model.conversation.ConversationAi;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationAiRepository extends MongoRepository<ConversationAi, String> {

  List<ConversationAi> findAllByUserId(Long id);
  Page<ConversationAi> findAllByUserId(Long userId, Pageable pageable);
}
