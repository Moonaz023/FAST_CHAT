package com.example.fast_chat.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageAiDTO {

  private String messageId;
  private String conversationId;
  private String senderId;
  private String senderName;
  private String content;
  private String tempId;

}
