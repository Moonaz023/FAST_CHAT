package com.example.fast_chat.dto.conversation;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationAiDTO {

  private String conversationId;
  private String useName;
  private LocalDateTime updatedAt;
  private int unreadCount;

}
