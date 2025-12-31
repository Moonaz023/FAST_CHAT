package com.example.fast_chat.dto.gemini;

public record Candidate(
        Content content,
        String finishReason,
        Integer index
) {}