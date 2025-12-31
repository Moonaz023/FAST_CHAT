package com.example.fast_chat.repository.conversation;


import com.example.fast_chat.model.conversation.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

}
