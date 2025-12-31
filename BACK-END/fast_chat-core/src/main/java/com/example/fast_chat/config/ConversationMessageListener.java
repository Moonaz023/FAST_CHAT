package com.example.fast_chat.config;


import com.example.fast_chat.model.conversation.ConversationMessageAi;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConversationMessageListener extends AbstractMongoEventListener<ConversationMessageAi> {

  @Override
  public void onBeforeConvert(BeforeConvertEvent<ConversationMessageAi> event) {
    ConversationMessageAi message = event.getSource();
    if (message.getCreatedAt() == null) {
      message.setCreatedAt(LocalDateTime.now());
    }
  }
}
