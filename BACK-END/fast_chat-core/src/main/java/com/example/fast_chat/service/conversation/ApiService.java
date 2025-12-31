package com.example.fast_chat.service.conversation;


import com.example.fast_chat.dto.gemini.GeminiResponse;
import com.example.fast_chat.dto.conversation.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class ApiService {

    @Value("${api.key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=%s";

    public String generateContent(String prompt) {
        String url = String.format(GEMINI_URL, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        return response.getBody();



    }
    public String chatWithGemini() throws JsonProcessingException {
        String url = String.format(GEMINI_URL, apiKey);
        List<MessageDTO> chatHistory = List.of(
                MessageDTO.builder()
                        .senderName("user")
                        .content("Hi, Introduce your self")
                        .sentAt(LocalDateTime.now())
                        .build(),

                MessageDTO.builder()
                        .senderName("model")
                        .content("""  
                  Hello there!\n\nI am a large language model, trained by Google.\n\n
                  My purpose is to assist you with a wide range of tasks by providing information, 
                  answering questions, generating creative content, and engaging in helpful conversation. 
                  Think of me as a knowledgeable assistant ready to help.\n\n
                  I don't have a name, personal experiences, or emotions like humans do, 
                  but I'm here to process information and interact with you to the best of my abilities.\n\n
                  So, please feel free to ask me anything or tell me what you'd like to discuss! 
                  How can I help you today?
                  """)
                        .sentAt(LocalDateTime.now())
                        .build(),

                MessageDTO.builder()
                        .senderName("user")
                        .content("i am moonaz, a java developer from Bangladesh")
                        .sentAt(LocalDateTime.now())
                        .build(),
                MessageDTO.builder()
                        .senderName("model")
                        .content("Nice to meet you, Moonaz!\n" +
                                "\n" +
                                "It's great to connect with a Java developer from Bangladesh. Java is a powerful and incredibly versatile language, used in so many different applications, from enterprise systems to Android apps.\n" +
                                "\n" +
                                "As a language model, I process a lot of information related to programming and development. So, if you have any questions about Java, programming concepts, architecture, or anything else related to software development, feel free to ask!\n" +
                                "\n" +
                                "What are you working on these days, or what brings you here today? I'm ready to help!")
                        .sentAt(LocalDateTime.now())
                        .build()
        );
        // Build the full contents array
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add entire history
        for (MessageDTO msg : chatHistory) {
            contents.add(Map.of(
                    "role", msg.getSenderName(),                      // "user" or "model"
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

//        // Add the newest user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Tell me about the the scope of my stack in my country"))
        ));

        // Send to Gemini
        Map<String, Object> request = Map.of("contents", contents);
        int maxRetries = 5;
        long initialDelayMs = 1000;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
        String jsonResponse = restTemplate.postForObject(url, request, String.class);

        GeminiResponse resp = mapper.readValue(jsonResponse, GeminiResponse.class);

        return resp.candidates().getFirst().content().parts().getFirst().text();
            } catch (HttpServerErrorException.ServiceUnavailable e) {
                // 503 error caught
                if (attempt == maxRetries - 1) {
                    // Last attempt failed, re-throw the exception
                    throw e;
                }

                // Calculate delay for the next attempt
                long delay = (long) (initialDelayMs * Math.pow(2, attempt));

                // Optional: Add jitter (a small random amount of time)
                long jitter = new Random().nextInt(1000);

                System.out.println("Model overloaded. Retrying in " + (delay + jitter) + "ms...");

                try {
                    Thread.sleep(delay + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted.", ie);
                }
            }
        }
        // Should be unreachable if the exception is re-thrown
        return null;
    }

}