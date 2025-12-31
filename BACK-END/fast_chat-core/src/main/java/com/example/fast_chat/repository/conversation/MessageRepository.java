package com.example.fast_chat.repository.conversation;

import com.example.fast_chat.model.conversation.Message;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

  @Query("""
    SELECT COUNT(m) FROM Message m
    JOIN ConversationParticipant p
        ON p.conversation.id = m.conversation.id
        AND p.user.id = :userId
    WHERE m.sender.id != :userId
    AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId)
    AND m.isDeleted = false
    AND p.isArchived = false
    """)
  Long countTotalUnreadMessages(@Param("userId") Long userId);
  @Query("""
    SELECT COUNT(m) FROM Message m
    JOIN ConversationParticipant p
        ON p.conversation.id = m.conversation.id
        AND p.user.id = :userId
    WHERE m.conversation.id = :conversationId
    AND m.sender.id != :userId
    AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId)
    AND m.isDeleted = false
    """)
  Long countUnreadMessages(
      @Param("conversationId") Long conversationId,
      @Param("userId") Long userId
  );

  List<Message> findByConversationId(Long conversationId);
  Page<Message> findByConversationId(Long conversationId, Pageable pageable);

  @Query("""
    SELECT MAX(m.id)
    FROM Message m
    WHERE m.conversation.id = :conversationId
      AND m.sender.id <> :userId
""")
  Long findLatestMessageId(
      @Param("conversationId") Long conversationId,
      @Param("userId") Long userId
  );

  @Query("""
    SELECT m.conversation.id, COUNT(m)
    FROM Message m
    JOIN ConversationParticipant cp
        ON cp.conversation.id = m.conversation.id
        AND cp.user.id = :userId
    WHERE m.sender.id <> :userId
      AND (cp.lastReadMessageId IS NULL OR m.id > cp.lastReadMessageId)
      AND m.isDeleted = false
    GROUP BY m.conversation.id
""")
  List<Object[]> getUnreadCounts(@Param("userId") Long userId);

  @Query("""
    SELECT COUNT(m)
    FROM Message m
    JOIN ConversationParticipant cp
        ON cp.conversation.id = m.conversation.id
        AND cp.user.id = :userId
    WHERE m.sender.id <> :userId
      AND (cp.lastReadMessageId IS NULL OR m.id > cp.lastReadMessageId)
      AND m.isDeleted = false
    """)
  Long countTotalUnreadForUser(@Param("userId") Long userId);

}
