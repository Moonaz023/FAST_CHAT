package com.example.fast_chat.model.conversation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "conversation_ai")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAi {

  @Id
  private String id;  // MongoDB uses ObjectId, stored as String (Spring Data handles conversion)

  @Field("ConversationName")
  private String ConversationName;

  @Field("type")
  private ConversationType type = ConversationType.DIRECT;

  @Field("createdAt")
  private LocalDateTime createdAt;

  @Field("updatedAt")
  private LocalDateTime updatedAt;

  @Field("userId")
  private Long userId;

  @Field("name")
  private String name;

  public enum ConversationType {
    DIRECT, GROUP
  }

  // Note: Timestamp management (createdAt/updatedAt) should be handled in service layer or using MongoDB lifecycle callbacks
  // Example using @PrePersist and @PreUpdate (requires import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener or similar setup)

  // @PrePersist
  // public void onCreate() {
  //     this.createdAt = LocalDateTime.now();
  //     this.updatedAt = LocalDateTime.now();
  // }
  //
  // @PreUpdate
  // public void onUpdate() {
  //     this.updatedAt = LocalDateTime.now();
  // }
}