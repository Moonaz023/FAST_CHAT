package com.example.fast_chat.service.security;

import com.example.fast_chat.model.User;
import com.example.fast_chat.repository.UserRepository;
import com.example.fast_chat.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class LinkedinOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    // Allow your frontend origin (change in production!)
    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1. Extract email from LinkedIn OAuth2 principal
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            log.warn("LinkedIn did not return email. Check scopes: r_emailaddress");
            sendError(response, "Email not provided by LinkedIn");
            return;
        }

        // 2. Find or create user (recommended: auto-register on first login)
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> registerNewUser(oAuth2User));

        // Optional: update profile picture/name if changed
        updateUserProfileIfNeeded(user, oAuth2User);

        // 3. Generate JWT
        String accessToken = jwtUtil.generateToken(email); // pass User or email as needed

        // 4. Build response payload
        Map<String, Object> payload = Map.of(
                "accessToken", accessToken,
                "tokenType", "Bearer",
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "profilePic",user.getPicture())
        );

        String jsonPayload = mapper.writeValueAsString(payload);

        // 5. Send back via postMessage + close popup
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script>
                    // Send auth result to parent window (your React app)
                    window.opener.postMessage(%s, "%s");
                    
                    // Optional: show success message before closing
                    document.write("<h3>Login successful! Closing...</h3>");
                    
                    // Close the popup
                    window.close();
                </script>
            </head>
            <body style="background:#667eea; color:white; font-family:Arial; text-align:center; padding:50px;">
                Logging you in...
            </body>
            </html>
            """.formatted(jsonPayload, ALLOWED_ORIGIN);

        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(html);
    }

    private User registerNewUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");


        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null ? name : email.split("@")[0]);

        log.info("Creating new user from LinkedIn: {}", email);
        return userRepository.save(newUser);
    }

    private void updateUserProfileIfNeeded(User user, OAuth2User oAuth2User) {
        String newPicture = oAuth2User.getAttribute("picture");
        String newName = oAuth2User.getAttribute("name");

        boolean changed = false;

        if (newName != null && !newName.equals(user.getName())) {
            user.setName(newName);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        Map<String, String> error = Map.of("error", message);
        String html = """
            <script>
                window.opener.postMessage(%s, "%s");
                window.close();
            </script>
            """.formatted(mapper.writeValueAsString(error), ALLOWED_ORIGIN);

        response.setContentType("text/html");
        response.getWriter().write(html);
    }
}
