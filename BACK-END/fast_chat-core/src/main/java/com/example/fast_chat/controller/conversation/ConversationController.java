package com.example.fast_chat.controller.conversation;

import com.example.fast_chat.dto.util.PageableList;
import com.example.fast_chat.dto.conversation.ConversationAiDTO;
import com.example.fast_chat.dto.conversation.ConversationDTO;
import com.example.fast_chat.dto.conversation.MessageAiDTO;
import com.example.fast_chat.dto.conversation.MessageDTO;
import com.example.fast_chat.mapper.MessageAiMapper;
import com.example.fast_chat.model.User;
import com.example.fast_chat.model.conversation.ConversationAi;
import com.example.fast_chat.model.conversation.Message;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.repository.conversation.ConversationAiRepository;
import com.example.fast_chat.repository.conversation.ConversationMessageAiRepository;
import com.example.fast_chat.repository.conversation.MessageRepository;
import com.example.fast_chat.service.conversation.ConversationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {
  private final ConversationService conversationService;
  private final MessageAiMapper messageAiMapper;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ConversationAiRepository conversationAiRepository;
  private final ConversationMessageAiRepository messageAiRepository;

  //@GetMapping("/getConversionList")
  public ResponseEntity<?> getConversionList(@AuthenticationPrincipal User user) {

    //conversationService.getOrCreateDirectConversation(user.getId(),9L);
    return ResponseEntity.ok(conversationService.getUserConversations(user.getId()));

  }


  @GetMapping("/getConversionList")
  public ResponseEntity<?> getPagedConversionList(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
    Page<ConversationDTO> result = conversationService.getUserConversationsPaged(user.getId(), pageable);

    return ResponseEntity.ok(result);
  }
  @GetMapping("/getAiConversionList")
  public ResponseEntity<?> getPagedAiConversionList(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

    Page<ConversationAi> pagedConversations = conversationAiRepository
        .findAllByUserId(user.getId(), pageable);

    Page<ConversationAiDTO> result = pagedConversations.map(conversationAi ->
        new ConversationAiDTO(
            conversationAi.getId(),
            conversationAi.getConversationName(),
            conversationAi.getCreatedAt(),
            0  // Replace with real unread count if available
        )
    );

/*
    List<ConversationAi> conversations = conversationAiRepository.findAllByUserId(user.getId());

    List<ConversationAiDTO> conversationListx = conversations.stream()
        .map(conversationAi -> new ConversationAiDTO(
            conversationAi.getId(),
            conversationAi.getConversationName(),  // You'll need to define this
            conversationAi.getCreatedAt(),        // or getLastMessageTime() if you track updates
            0       // or calculate it if not stored
        ))
        .toList();
    // Dummy data
    List<ConversationDTO> dummyList = List.of(
        new ConversationDTO(null, "ChatGPT", LocalDateTime.now().minusHours(2), 3),
        new ConversationDTO(null, "Claude", LocalDateTime.now().minusDays(1), 0),
        new ConversationDTO(null, "Grok", LocalDateTime.now().minusMinutes(15), 7),
        new ConversationDTO(null, "Gemini", LocalDateTime.now().minusHours(5), 1),
        new ConversationDTO(null, "Llama 3", LocalDateTime.now().minusDays(2), 0)
    );

    // Manual pagination (because we have no DB)
    int start = page * size;
    int end   = Math.min(start + size, conversationList.size());

    List<ConversationAiDTO> pageContent = start < conversationList.size()
        ? conversationList.subList(start, end)
        : Collections.emptyList();

    Page<ConversationAiDTO> result = new PageImpl<>(
        pageContent,
        PageRequest.of(page, size, Sort.by("updatedAt").descending()),
        conversationList.size()
    );*/

    return ResponseEntity.ok(result);
  }
  @GetMapping("/unread/total")
  public ResponseEntity<Long> getTotalUnreadCount(@AuthenticationPrincipal User user) {
    Long total =  messageRepository.countTotalUnreadForUser(user.getId());
    return ResponseEntity.ok(total != null ? total : 0L);
  }
  @GetMapping("/findOrNull/{targetUserId}")
  public ResponseEntity<Map<String, Long>> findExistingConversation(
      @PathVariable Long targetUserId,
      @AuthenticationPrincipal User user
  ) {
    User me = userRepository.findByEmail(user.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    Long conversationId = conversationService.findDirectConversation(me.getId(), targetUserId);

    Map<String, Long> body = new HashMap<>();
    body.put("conversationId", conversationId); // can be null

    return ResponseEntity.ok(body);
  }



  //@GetMapping("/{conversationId}/messages")
  public List<MessageDTO> getMessages(
      @PathVariable Long conversationId,
      Authentication authentication) {

    List<MessageDTO> messagesResponse=new ArrayList<>();
    List<Message> messages = messageRepository.findByConversationId(conversationId);
    for(Message m:messages){
      MessageDTO dto=new MessageDTO();
      dto.setMessageId(m.getId());
      dto.setConversationId(m.getConversation().getId());
      dto.setContent(m.getContent());
      dto.setSenderId(m.getSender().getId());
      messagesResponse.add(dto);
    }

    return messagesResponse;
  }
  @GetMapping("/{conversationId}/messages")
  public ResponseEntity<Page<MessageDTO>> getMessagesPage(
      @PathVariable Long conversationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size,
      Authentication authentication) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

    Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);

    List<MessageDTO> dtos = messagePage.getContent().stream()
        .map(m -> {
          MessageDTO dto = new MessageDTO();
          dto.setMessageId(m.getId());
          dto.setConversationId(m.getConversation().getId());
          dto.setContent(m.getContent());
          dto.setSenderId(m.getSender().getId());
          return dto;
        })
        .toList();

    return ResponseEntity.ok(new PageImpl<>(dtos, pageable, messagePage.getTotalElements()));
  }

  @GetMapping("/ai/{conversationId}/messages")
  //@GetMapping("/ai-messages/{conversationId}")
  public ResponseEntity<Mono<PageableList<MessageAiDTO>>> getAiMessagesPage(
      @PathVariable String conversationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size,
      Authentication authentication) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

    Mono<PageableList<MessageAiDTO>> messagePage =
        messageAiRepository.countByConversationId(conversationId)
            .flatMap(total ->
                messageAiRepository.findByConversationId(conversationId, pageable)
                    .map(messageAiMapper::toDto)
                    .collectList()
                    .map(content ->
                        new PageableList<>(content, total, page, size)
                    )
            );

    return ResponseEntity.ok(messagePage);
  }


  @PostMapping("/create")
  public ResponseEntity<Map<String, Long>> createConversation(
      @RequestBody Map<String, Long> body,
      @AuthenticationPrincipal User user
  ) {
    Long targetUserId = body.get("targetUserId");
    if (targetUserId == null) {
      return ResponseEntity.badRequest().build();
    }

    // Current logged-in user
    User me = userRepository.findByEmail(user.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Check if conversation already exists
    Long existingId = conversationService.findDirectConversation(me.getId(), targetUserId);
    if (existingId != null) {
      return ResponseEntity.ok(Map.of("id", existingId));
    }

    // Otherwise, create new one
    Long newId = conversationService.createDirectConversation(me, targetUserId);

    return ResponseEntity.ok(Map.of("id", newId));
  }


}
