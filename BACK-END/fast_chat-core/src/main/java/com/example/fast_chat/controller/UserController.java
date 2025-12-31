package com.example.fast_chat.controller;

import com.example.fast_chat.model.User;
import com.example.fast_chat.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getUserS() {
        return ResponseEntity.ok("Hello User");
    }

    @GetMapping("/search")
    public ResponseEntity<?> findUsers(
        @AuthenticationPrincipal User user,
        @RequestParam String query) {
        List<User> users=userRepository.searchByNameOrEmail(query);
        return ResponseEntity.ok(users);
    }
}
