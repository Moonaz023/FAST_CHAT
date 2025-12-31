//package com.example.auth.service.security;
//
//import com.example.auth.model.User;
//import com.example.auth.repo.UserRepository;
//import com.example.auth.util.JwtUtil;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.LinkedHashMap;
//import java.util.Map;
//@Component
//public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
//
//    private final JwtUtil jwtUtil;
//    private final UserRepository userRepository;
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    public OAuth2SuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
//        this.jwtUtil = jwtUtil;
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException {
//
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//
//        User user = userRepository.findByEmail(email).orElseThrow();
//
//        String token = jwtUtil.generateToken(email);
//
//        Map<String, Object> body = Map.of(
//                "accessToken", token,
//                "type", "Bearer",
//                "email", user.getEmail(),
//                "username", user.getName(),
//                "picture", user.getPicture(),
//                "id", user.getId()
//        );
//
//        String json = mapper.writeValueAsString(body);
//
//        String html = """
//            <!DOCTYPE html>
//            <html><body style="background:#667eea">
//              <script>
//                window.opener.postMessage(%s, "http://localhost:3000");
//                window.close();
//              </script>
//            </body></html>
//            """.formatted(json);
//
//        response.setContentType("text/html");
//        response.getWriter().write(html);
//    }
//}