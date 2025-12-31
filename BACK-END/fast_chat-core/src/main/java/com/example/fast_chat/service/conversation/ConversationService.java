package com.example.fast_chat.service.conversation;

import com.example.fast_chat.dto.conversation.ConversationDTO;
import com.example.fast_chat.model.User;
import com.example.fast_chat.model.conversation.Conversation;
import com.example.fast_chat.model.conversation.Conversation.ConversationType;
import com.example.fast_chat.model.conversation.ConversationParticipant;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.repository.conversation.ConversationParticipantRepository;
import com.example.fast_chat.repository.conversation.ConversationRepository;
import com.example.fast_chat.repository.conversation.MessageRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final ConversationParticipantRepository participantRepository;
  private final UserRepository userRepository;

  /*public List<ConversationDTO> getUserConversations(Long userId) {
    // Get all conversations for user
    List<Conversation> conversations = conversationRepository.findAllByUserId(userId);

    // Convert to DTOs with all details
    return conversations.stream()
        .map(conversation -> buildConversationDTO(conversation, userId))
        .collect(Collectors.toList());
  }
  */
  public List<ConversationDTO> getUserConversations(Long userId) {

    List<Conversation> conversations = conversationRepository.findAllByUserId(userId);

    // Load all unread counts in ONE DB call
    Map<Long, Integer> unreadMap = messageRepository
        .getUnreadCounts(userId)
        .stream()
        .collect(Collectors.toMap(
            row -> (Long) row[0],
            row -> ((Long) row[1]).intValue()
        ));

    return conversations.stream()
        .map(conv -> buildConversationDTO(conv, userId, unreadMap))
        .collect(Collectors.toList());
  }
  public Page<ConversationDTO> getUserConversationsPaged(Long userId, Pageable pageable) {
    Page<Conversation> page = conversationRepository.findAllByUserIdPaged(userId, pageable);


    // Still load ALL unread counts in one query (super fast!)
    Map<Long, Integer> unreadMap = messageRepository
        .getUnreadCounts(userId)
        .stream()
        .collect(Collectors.toMap(
            row -> (Long) row[0],
            row -> ((Long) row[1]).intValue()
        ));

    List<ConversationDTO> dtos = page.getContent().stream()
        .map(conv -> buildConversationDTO(conv, userId, unreadMap))
        .toList();

    return new PageImpl<>(dtos, pageable, page.getTotalElements());
  }


  private ConversationDTO buildConversationDTO(
      Conversation conversation,
      Long userId,
      Map<Long, Integer> unreadMap
  ) {
    ConversationDTO dto = new ConversationDTO();
    dto.setConversationId(conversation.getId());
    dto.setUpdatedAt(conversation.getUpdatedAt());

    // Set unread count using map
    dto.setUnreadCount(unreadMap.getOrDefault(conversation.getId(), 0));

    // Set other user's name
    List<ConversationParticipant> others =
        participantRepository.findOtherParticipants(conversation.getId(), userId);

    if (!others.isEmpty()) {
      dto.setUseName(others.getFirst().getUser().getName());
    }

    return dto;
  }

  @Transactional
  public ConversationDTO getOrCreateDirectConversation(Long userId1, Long userId2) {
    // Check if conversation already exists
    Optional<Conversation> existing = conversationRepository
        .findDirectConversation(userId1, userId2);

    if (existing.isPresent()) {
      return buildConversationDTO(existing.get(), userId1,new HashMap<>());
    }

    // CREATE NEW CONVERSATION
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.DIRECT);
    conversation.setCreatedAt(LocalDateTime.now());
    conversation.setUpdatedAt(LocalDateTime.now());
    conversation = conversationRepository.save(conversation);

    // ADD MOON AS PARTICIPANT
    ConversationParticipant moonParticipant = new ConversationParticipant();
    moonParticipant.setConversation(conversation);
    moonParticipant.setUser(userRepository.findById(userId1).orElseThrow());
    moonParticipant.setJoinedAt(LocalDateTime.now());
    participantRepository.save(moonParticipant);

    // ADD RAHMAN AS PARTICIPANT
    ConversationParticipant rahmanParticipant = new ConversationParticipant();
    rahmanParticipant.setConversation(conversation);
    rahmanParticipant.setUser(userRepository.findById(userId2).orElseThrow()); // Rahman
    rahmanParticipant.setJoinedAt(LocalDateTime.now());
    participantRepository.save(rahmanParticipant);

    return buildConversationDTO(conversation, userId1,new HashMap<>());

  }

  public Long findDirectConversation(Long me, Long targetUserId) {

      return conversationRepository.findDirectConversation(me, targetUserId)
          .map(Conversation::getId)
          .orElse(null);

  }

  @Transactional
  public Long createDirectConversation(User userA, Long targetUser) {

    // Create the conversation
    Conversation conv = new Conversation();
    conv.setCreatedAt(LocalDateTime.now());
    conversationRepository.save(conv);

    User userB = userRepository.findById(targetUser).orElseThrow();

    // Add participants
    participantRepository.save( ConversationParticipant.builder().conversation(conv).user(userA).build());
    participantRepository.save( ConversationParticipant.builder().conversation(conv).user(userB).build());


    return conv.getId();
  }
}