package com.example.fast_chat.dto.gemini;

import java.util.List;

public record GeminiResponse(
        String responseId,
        List<Candidate> candidates,
        UsageMetadata usageMetadata,
        String modelVersion
) {}


record UsageMetadata(
        Integer promptTokenCount,
        Integer candidatesTokenCount,
        Integer totalTokenCount,
        List<PromptTokenDetail> promptTokensDetails,
        Integer thoughtsTokenCount
) {}

record PromptTokenDetail(
        String modality,
        Integer tokenCount
) {}
