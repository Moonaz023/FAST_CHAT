package com.example.fast_chat.service.conversation;

import com.example.fast_chat.dto.conversation.MessageDTO;
import com.example.fast_chat.model.conversation.Conversation;
import com.example.fast_chat.model.conversation.ConversationParticipant;
import com.example.fast_chat.model.conversation.Message;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.repository.conversation.ConversationParticipantRepository;
import com.example.fast_chat.repository.conversation.ConversationRepository;
import com.example.fast_chat.repository.conversation.MessageRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository participantRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @Transactional
  public void sendMessage(Long senderId, Long conversationId, String content, String tempId) {

    // 1. VALIDATE: Check if sender is participant
//    if (!conversationRepository.isUserParticipant(conversationId, senderId)) {
//      throw new IllegalArgumentException("User not in conversation");
//    }

    // 2. SAVE MESSAGE TO DATABASE
    Message message = new Message();
    message.setConversation(conversationRepository.findById(conversationId).orElseThrow());
    message.setSender(userRepository.findById(senderId).orElseThrow());
    message.setContent(content);
    message.setSentAt(LocalDateTime.now());
    message = messageRepository.save(message);

    // 3. UPDATE CONVERSATION'S UPDATED_AT
    Conversation conversation = message.getConversation();
    conversation.setUpdatedAt(LocalDateTime.now());
    conversationRepository.save(conversation);

    // 4. BUILD MESSAGE DTO
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setMessageId(message.getId());
    messageDTO.setConversationId(conversationId);
    messageDTO.setSenderId(senderId);
    messageDTO.setSenderName(message.getSender().getName());
    messageDTO.setContent(content);
    messageDTO.setSentAt(message.getSentAt());
    messageDTO.setTempId(tempId); // For Moon to match with optimistic UI

    // 5. GET ALL PARTICIPANTS
    List<ConversationParticipant> participants =
        participantRepository.findAllByConversationId(conversationId);

    // 6. SEND MESSAGE TO ALL PARTICIPANTS VIA WEBSOCKET
    for (ConversationParticipant participant : participants) {
      String userEmail = participant.getUser().getEmail();

      // Send to each user's personal queue
      messagingTemplate.convertAndSendToUser(
          userEmail,
          "/queue/messages",
          messageDTO
      );

      System.out.println("✅ Message sent to: " + userEmail);
    }

    // 7. UPDATE UNREAD COUNTS FOR OTHER USERS
    for (ConversationParticipant participant : participants) {
      if (!participant.getUser().getId().equals(senderId)) {
        // Calculate unread count for this user
        Long unreadCount = messageRepository.countUnreadMessages(
            conversationId,
            participant.getUser().getId()
        );



        messagingTemplate.convertAndSendToUser(
            participant.getUser().getEmail(),
            "/queue/notify",
            1L
        );

        System.out.println("🔔 Unread notification sent to: " +
            participant.getUser().getEmail() + " count: " + unreadCount);
      }
    }
  }
}