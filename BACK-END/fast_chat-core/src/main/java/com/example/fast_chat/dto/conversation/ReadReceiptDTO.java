package com.example.fast_chat.dto.conversation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadReceiptDTO {

private Long userId;
private Long messageId;
private Long conversationId;

}
