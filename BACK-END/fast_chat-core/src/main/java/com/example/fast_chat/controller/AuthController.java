package com.example.fast_chat.controller;

import com.example.fast_chat.dto.security.LoginRequest;
import com.example.fast_chat.dto.security.RegisterRequest;
import com.example.fast_chat.model.User;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.service.conversation.ApiService;
import com.example.fast_chat.service.message.NotificationService;
import com.example.fast_chat.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


  private final NotificationService notificationService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;
  private final ApiService apiService;


  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req) {
    // 1 Check existing user
    if (userRepository.findByEmail(req.email()).isPresent()) {
      throw new BadCredentialsException("Email already taken");
    }

    // 2 Create user
    User user = new User();
    user.setEmail(req.email());
    user.setName(req.name());
    user.setPassword(passwordEncoder.encode(req.password()));
    userRepository.save(user);

    // 3 Generate JWT
    String token = jwtUtil.generateToken(user.getEmail());

    // 4 Build response object
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("accessToken", token);
    response.put("type", "Bearer");
    response.put("email", user.getEmail());
    response.put("username", user.getName());
    response.put("id", user.getId());
    response.put("status", "success");

    // 5 Return as JSON (no cookie)
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
    //  Authenticate
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.email(), req.password())
    );
    SecurityContextHolder.getContext().setAuthentication(auth);

    //  Fetch user
    User user = userRepository.findByEmail(req.email())
        .orElseThrow(() -> new BadCredentialsException("User not found"));

    //  Generate JWT
    String token = jwtUtil.generateToken(user.getEmail());

    //  Build response object
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("accessToken", token);
    response.put("type", "Bearer");
    response.put("email", user.getEmail());
    response.put("username", user.getName());
    response.put("id", user.getId());
    response.put("status", "success");
    response.put("profilePic",user.getPicture());

    //  Return as JSON (no cookie)
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    // Clear the cookie
    ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", "")
        .httpOnly(true)
        .secure(false)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return ResponseEntity.ok("Logged out successfully");
  }

  // Helper method to set cookie
  private void setTokenCookie(HttpServletResponse response, String token) {
    ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
        .httpOnly(true)
        .secure(false)   // false for localhost, true for production with HTTPS
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofHours(1))
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    System.out.println(" Cookie set: " + cookie);
  }

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(
      @CookieValue(name = "ACCESS_TOKEN", required = false) String token) {
    if (token == null) {
      return ResponseEntity.status(401).body("No token found");
    }
    try {
      String email = jwtUtil.extractUsername(token);
      User user = userRepository.findByEmail(email).orElseThrow();
      return ResponseEntity.ok(user);
    } catch (Exception e) {
      return ResponseEntity.status(401).body("Invalid token");
    }
  }

  @GetMapping("/notify")
  public ResponseEntity<?> notify(HttpServletResponse response) {
    notificationService.sendDirectNotification("Testing....!");
    return ResponseEntity.ok("Test message send");
  }

  @GetMapping("/ai-test")
  public ResponseEntity<?> aiTest() throws JsonProcessingException {
    return ResponseEntity.ok(apiService.chatWithGemini());
  }


}
