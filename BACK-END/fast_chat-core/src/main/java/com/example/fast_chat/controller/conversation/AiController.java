package com.example.fast_chat.controller.conversation;


import com.example.fast_chat.dto.gemini.GeminiRequest;
import com.example.fast_chat.model.User;
import com.example.fast_chat.model.conversation.ConversationAi;
import com.example.fast_chat.model.conversation.ConversationMessageAi;
import com.example.fast_chat.repository.conversation.ConversationAiRepository;
import com.example.fast_chat.repository.conversation.ConversationMessageAiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
  
  private final ConversationAiRepository conversationAiRepository;
  private final ConversationMessageAiRepository messageAiRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WebClient webClient;
  @Value("${api.key}")
  private String apiKey;


  @GetMapping(value = "/stream-story", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamGeminiResponseTest(Authentication authentication) {

    System.out.println(" Controller called for user: " + authentication.getName());

    String prompt = "Hi, I am Moonaz from Bangladesh. Tell me a short story about a Java developer who meets an AI in Dhaka.";

    Map<String, Object> request = Map.of(
        "contents", List.of(
            Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
            )
        )
    );

    StringBuilder fullResponse = new StringBuilder();

    return webClient.post()
        .uri(
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:streamGenerateContent?key="
                + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(JsonNode.class)
        .flatMap(node -> {
          JsonNode textNode = node.path("candidates")
              .path(0)
              .path("content")
              .path("parts")
              .path(0)
              .path("text");

          if (!textNode.isMissingNode()) {
            String chunk = textNode.asText();
            fullResponse.append(chunk);

            //  CRITICAL: Log each chunk to see what we're receiving
            System.out.println("📦 Chunk [" + chunk.length() + " chars]: " + chunk);

            //  Send the chunk exactly as received from Gemini
            return Flux.just(chunk + "\n\n");
          }
          return Flux.empty();
        })
        .doOnComplete(() -> {
          System.out.println(" Stream completed for: " + authentication.getName());
          System.out.println(" Total length: " + fullResponse.length());
        })
        .onErrorResume(err -> {
          System.err.println(" Stream error: " + err.getMessage());

          String errorMessage;

          if (err.getMessage() != null && err.getMessage().contains("429")) {
            errorMessage = "Server is busy due to rate limits. Please try again in a few minutes.";
          } else if (err.getMessage() != null && err.getMessage().contains("Too Many Requests")) {
            errorMessage = "Too many requests. Please wait a moment and try again.";
          } else if (err.getMessage() != null && err.getMessage().contains("timeout")) {
            errorMessage = "Request timed out. Please try again.";
          } else {
            errorMessage = "An error occurred: " + err.getMessage();
          }

          return Flux.just(
              "[ERROR_START]\n\n",
              errorMessage + "\n\n",
              "[ERROR_END]\n\n"
          );
        });
  }
  
  //  Generate a short, descriptive title using Gemini API
  private String generateConversationTitle(String userPrompt) {
    try {
      // Create a prompt to generate a short title
      String titlePrompt = "Generate a very short title (maximum 4-5 words) for a conversation that starts with this message: \""
          + userPrompt + "\". Return ONLY the title, nothing else.";

      Map<String, Object> request = Map.of(
          "contents", List.of(
              Map.of(
                  "role", "user",
                  "parts", List.of(Map.of("text", titlePrompt))
              )
          )
      );

      // Make synchronous call to get title
      JsonNode response = webClient.post()
          .uri("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block(Duration.ofSeconds(5)); // 5 second timeout

      if (response != null) {
        JsonNode textNode = response.path("candidates")
            .path(0)
            .path("content")
            .path("parts")
            .path(0)
            .path("text");

        if (!textNode.isMissingNode()) {
          String title = textNode.asText().trim();

          // Clean up the title (remove quotes, limit length)
          title = title.replaceAll("^\"|\"$", ""); // Remove surrounding quotes
          title = title.replaceAll("^'|'$", ""); // Remove surrounding single quotes

          if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
          }

          System.out.println(" Generated title: " + title);
          return title;
        }
      }
    } catch (Exception e) {
      System.err.println(" Failed to generate title: " + e.getMessage());
    }

    // Fallback: Use first few words of user prompt
    return generateFallbackTitle(userPrompt);
  }

  //  Fallback method if AI title generation fails
  private String generateFallbackTitle(String userPrompt) {
    // Take first 40 characters or first sentence
    String title = userPrompt.length() > 40
        ? userPrompt.substring(0, 40) + "..."
        : userPrompt;

    // If there's a sentence ending, use that
    int periodIndex = userPrompt.indexOf('.');
    int questionIndex = userPrompt.indexOf('?');
    int exclamationIndex = userPrompt.indexOf('!');

    int endIndex = Math.min(
        periodIndex > 0 ? periodIndex : Integer.MAX_VALUE,
        Math.min(
            questionIndex > 0 ? questionIndex : Integer.MAX_VALUE,
            exclamationIndex > 0 ? exclamationIndex : Integer.MAX_VALUE
        )
    );

    if (endIndex < 50 && endIndex != Integer.MAX_VALUE) {
      title = userPrompt.substring(0, endIndex + 1);
    }

    return title;
  }

  // Reactive method to save messages 
  private Mono<ConversationMessageAi> saveMessageReactive(String conversationId, String role, String content) {
    ConversationMessageAi message = ConversationMessageAi.builder()
        .conversationId(conversationId)
        .role(role)
        .content(content)
        .createdAt(LocalDateTime.now())
        .build();

    return messageAiRepository.save(message)
        .doOnSuccess(saved ->
            System.out.println(" Saved " + role + " message: " + saved.getId()
                + " [" + content.length() + " chars]"))
        .doOnError(error ->
            System.err.println(" Failed to save message: " + error.getMessage()))
        .onErrorResume(error -> Mono.empty()); // Don't fail stream if save fails
  }
  
  private Mono<List<Map<String, Object>>> buildConversationHistory(
      String conversationId
  ) {

    return messageAiRepository
        .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId)
        .collectList()
        .map(messages -> {

          // Reverse to restore chronological order
          Collections.reverse(messages);

          List<Map<String, Object>> contents = new ArrayList<>();

          for (ConversationMessageAi msg : messages) {
            contents.add(
                Map.of(
                    "role", msg.getRole().equals("user") ? "user" : "model",
                    "parts", List.of(
                        Map.of("text", msg.getContent())
                    )
                )
            );
          }

          return contents;
        });
  }
  @PostMapping(
      value = "/stream-story",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_EVENT_STREAM_VALUE
  )
  public Flux<String> streamGeminiResponse_store(
      Authentication authentication,
      @AuthenticationPrincipal User user,
      @RequestBody GeminiRequest requestMono
  ) {

    String username = (authentication != null)
        ? authentication.getName()
        : "anonymous";

    System.out.println(" Controller called for user: " + username);

    String prompt = requestMono.prompt();

    if (prompt == null || prompt.trim().isEmpty()) {
      return Flux.just(
          "[ERROR_START]\n\n",
          "Prompt is required.\n\n",
          "[ERROR_END]\n\n"
      );
    }

    // ================= Conversation =================

    String conversationId = requestMono.conversationId();
    boolean isNewConversation = (conversationId == null || conversationId.trim().isEmpty());

    String conversationName = null;

    if (isNewConversation) {
      conversationName = generateConversationTitle(prompt);

      ConversationAi conversationAi = new ConversationAi();
      conversationAi.setConversationName(conversationName);
      conversationAi.setUserId(user.getId());
      conversationAi.setCreatedAt(LocalDateTime.now());
      conversationAi.setUpdatedAt(LocalDateTime.now());

      ConversationAi newConversation = conversationAiRepository.save(conversationAi);
      conversationId = newConversation.getId();

      System.out.println(" Created new conversation: " + conversationId + " - " + conversationName);
    } else {
      System.out.println(" Continuing conversation: " + conversationId);
    }

    final String finalConversationId = conversationId;
    final String finalConversationName = conversationName;

    // ================= Save User Message =================

    saveMessageReactive(finalConversationId, "user", prompt)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            saved -> System.out.println(" User message saved: " + saved.getId()),
            error -> System.err.println(" Failed to save user message: " + error.getMessage())
        );

    StringBuilder fullResponse = new StringBuilder();

    // ================= Metadata (NEW conversation only) =================

    Flux<String> metadataFlux = isNewConversation
        ? Flux.just(
        "[CONVERSATION_ID:" + finalConversationId + "]\n\n",
        "[CONVERSATION_NAME:" + finalConversationName + "]\n\n"
    )
        : Flux.empty();

    // ================= Build Context + Stream =================

    return buildConversationHistory(finalConversationId)
        .flatMapMany(historyContents -> {

          // Append current prompt
          historyContents.add(
              Map.of(
                  "role", "user",
                  "parts", List.of(
                      Map.of("text", prompt)
                  )
              )
          );

          Map<String, Object> request = Map.of(
              "contents", historyContents
          );

          return Flux.concat(
              metadataFlux,

              webClient.post()
                  .uri(
                      "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:streamGenerateContent?key="
                          + apiKey)
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(request)
                  .retrieve()
                  .bodyToFlux(JsonNode.class)
                  .flatMap(node -> {
                    JsonNode textNode = node.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text");

                    if (!textNode.isMissingNode()) {
                      String chunk = textNode.asText();
                      fullResponse.append(chunk);

                      System.out.println(
                          "Chunk [" + chunk.length() + " chars]: " + chunk
                      );

                      return Flux.just(chunk + "\n\n");
                    }
                    return Flux.empty();
                  })
                  .doOnComplete(() -> {
                    System.out.println(" Stream completed for: " + username);
                    System.out.println(" Total length: " + fullResponse.length());
                    System.out.println("Conversation ID: " + finalConversationId);

                    String aiResponse = fullResponse.toString();
                    if (!aiResponse.isEmpty()) {
                      saveMessageReactive(finalConversationId, "model", aiResponse)
                          .subscribeOn(Schedulers.boundedElastic())
                          .subscribe(
                              saved -> System.out.println(" AI message saved: " + saved.getId()),
                              error -> System.err.println(" Failed to save AI message: " + error.getMessage())
                          );

                      conversationAiRepository.findById(finalConversationId).ifPresent(conv -> {
                        conv.setUpdatedAt(LocalDateTime.now());
                        conversationAiRepository.save(conv);
                      });
                    }
                  })
                  .onErrorResume(err -> {
                    System.err.println(" Stream error: " + err.getMessage());

                    String errorMessage;
                    if (err.getMessage() != null && err.getMessage().contains("429")) {
                      errorMessage = "Server is busy due to rate limits. Please try again in a few minutes.";
                    } else if (err.getMessage() != null && err.getMessage().contains("Too Many Requests")) {
                      errorMessage = "Too many requests. Please wait a moment and try again.";
                    } else if (err.getMessage() != null && err.getMessage().contains("timeout")) {
                      errorMessage = "Request timed out. Please try again.";
                    } else {
                      errorMessage = "An error occurred: " + err.getMessage();
                    }

                    return Flux.just(
                        "[ERROR_START]\n\n",
                        errorMessage + "\n\n",
                        "[ERROR_END]\n\n"
                    );
                  })
          );
        });
  }
  
}
