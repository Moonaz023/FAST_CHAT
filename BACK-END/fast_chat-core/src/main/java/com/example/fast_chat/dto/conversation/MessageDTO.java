package com.example.fast_chat.dto.conversation;

import java.time.LocalDateTime;
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
public class MessageDTO {

    private Long messageId;
  private Long conversationId;
  private Long senderId;
  private String senderName;
  private String content;
  private LocalDateTime sentAt;
  private String tempId;

}
