package com.example.fast_chat.dto.gemini;

import java.util.List;

public record Content(
        List<Part> parts,
        String role
) {}
