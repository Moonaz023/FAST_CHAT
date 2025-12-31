package com.example.fast_chat.dto.conversation;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
  private Long messageId;
  private Long conversationId;
  private Long senderId;
  private String senderName;
  private String content;
  private LocalDateTime sentAt;
  private String tempId;

}
