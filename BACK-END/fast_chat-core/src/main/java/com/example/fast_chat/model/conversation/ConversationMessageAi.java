package com.example.fast_chat.model.conversation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "conversation_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageAi {

  @Id
  private String id; // MongoDB ObjectId as String

  @Field("conversationId")
  private String conversationId; // References ConversationAi._id (String)

  @Field("role")
  private String role; // "user" or "model" (AI)

  @Field("content")
  private String content; // The prompt or response text

  @Field("createdAt")
  private LocalDateTime createdAt;

  // Optional: if you want to store token usage, safety ratings, etc.
  // private Integer promptTokens;
  // private Integer responseTokens;

  // Lifecycle callback to set timestamp on insert
  // (Requires @PrePersist support via MongoEventListener or manual setting)
  public void setCreatedAtOnCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}