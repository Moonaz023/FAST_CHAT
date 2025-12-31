package com.example.fast_chat.repository.conversation;

import com.example.fast_chat.model.conversation.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  @Query("""
    SELECT DISTINCT c FROM Conversation c
    JOIN FETCH c.participants p
    JOIN FETCH p.user u
    WHERE c.id IN (
        SELECT cp.conversation.id FROM ConversationParticipant cp
        WHERE cp.user.id = :userId
        AND cp.isArchived = false
    )
    ORDER BY c.updatedAt DESC
    """)
  List<Conversation> findAllByUserId(@Param("userId") Long userId);

  @Query("""
    SELECT DISTINCT c FROM Conversation c
    JOIN FETCH c.participants p
    JOIN FETCH p.user u
    WHERE c.id IN (
        SELECT cp.conversation.id FROM ConversationParticipant cp
        WHERE cp.user.id = :userId
          AND cp.isArchived = false
    )
    ORDER BY c.updatedAt DESC
    """)
  Page<Conversation> findAllByUserIdPaged(@Param("userId") Long userId, Pageable pageable);

  @Query("""
        SELECT c FROM Conversation c
        JOIN c.participants p1
        JOIN c.participants p2
        WHERE c.type = 'DIRECT'
        AND p1.user.id = :userId1
        AND p2.user.id = :userId2
        """)
  Optional<Conversation> findDirectConversation(
      @Param("userId1") Long userId1,
      @Param("userId2") Long userId2
  );
}
