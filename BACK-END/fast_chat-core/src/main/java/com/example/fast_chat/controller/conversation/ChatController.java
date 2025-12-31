package com.example.fast_chat.controller.conversation;

import com.example.fast_chat.dto.conversation.ReadReceiptDTO;
import com.example.fast_chat.dto.conversation.SendMessageRequest;
import com.example.fast_chat.model.User;
import com.example.fast_chat.model.conversation.ConversationParticipant;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.repository.conversation.ConversationParticipantRepository;
import com.example.fast_chat.repository.conversation.MessageRepository;
import com.example.fast_chat.service.conversation.MessageService;
import jakarta.transaction.Transactional;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {


  private final MessageService messageService;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;

  @MessageMapping("/chat.send")
  public void handleMessage(SendMessageRequest request, Principal principal) {
    System.out.println("Backend get Hit");
    String email = principal.getName();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Long senderId = user.getId();
    System.out.println(senderId+","+request.getConversationId()+","+
    request.getContent()+","+
    request.getTempId());
    messageService.sendMessage(
        senderId,
        request.getConversationId(),
        request.getContent(),
        request.getTempId()
    );
  }

  @Transactional
  @MessageMapping("/chat.read")
  public void markAsRead(
      @Payload ReadReceiptDTO receipt,
      Principal principal) {

    System.out.println("chat.read : Backend get Hit");
    String email = principal.getName();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    ConversationParticipant participant = conversationParticipantRepository
        .findByConversationIdAndUserId(receipt.getConversationId(), user.getId())
        .orElseThrow();

    if(receipt.getMessageId()==null)
    {
      Long latestOtherMessageId =messageRepository.findLatestMessageId(receipt.getConversationId(),user.getId());
      receipt.setMessageId(latestOtherMessageId);
    }

    if (receipt.getMessageId()==null) {
      log.info("No messages from other participants. Nothing to mark as read.");
      return;
    }

    // Only update if the new message is newer than what we've read
    if (receipt.getMessageId() > (participant.getLastReadMessageId() != null
        ? participant.getLastReadMessageId() : 0)) {

      participant.setLastReadMessageId(receipt.getMessageId());
      conversationParticipantRepository.save(participant);
    }
  }
}