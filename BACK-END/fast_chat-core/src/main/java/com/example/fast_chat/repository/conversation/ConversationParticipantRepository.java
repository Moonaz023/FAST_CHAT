package com.example.fast_chat.repository.conversation;

import com.example.fast_chat.model.conversation.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

  @Query("""
        SELECT p FROM ConversationParticipant p
        JOIN FETCH p.user
        WHERE p.conversation.id = :conversationId
        AND p.user.id != :userId
        """)
  List<ConversationParticipant> findOtherParticipants(
      @Param("conversationId") Long conversationId,
      @Param("userId") Long userId
  );

  List<ConversationParticipant> findAllByConversationId(Long conversationId);

  @Query("""
    SELECT cp
    FROM ConversationParticipant cp
    JOIN FETCH cp.conversation c
    JOIN FETCH cp.user u
    WHERE c.id = :conversationId
      AND u.id = :userId
""")
  Optional<ConversationParticipant > findByConversationIdAndUserId(long conversationId, long userId);


}
