package com.example.fast_chat.mapper;

import com.example.fast_chat.dto.conversation.MessageAiDTO;
import com.example.fast_chat.model.conversation.ConversationMessageAi;
import org.springframework.stereotype.Component;

@Component
public class MessageAiMapper {

  public MessageAiDTO toDto(ConversationMessageAi entity){

    return new MessageAiDTO(entity.getId(), entity.getConversationId(),"model",entity.getRole(),
        entity.getContent(),null);
  }

}
