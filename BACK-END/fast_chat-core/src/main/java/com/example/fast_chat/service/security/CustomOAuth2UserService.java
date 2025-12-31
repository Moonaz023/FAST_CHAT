//package com.example.auth.service.security;
//
//import com.example.auth.model.User;
//import com.example.auth.repo.UserRepository;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//
//@Service
//public class CustomOAuth2UserService extends DefaultOAuth2UserService {
//
//    private final UserRepository userRepository;
//
//    public CustomOAuth2UserService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
//        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
//
//        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
//        String email = oAuth2User.getAttribute("email");
//
//        if (email == null || email.isBlank()) {
//            throw new OAuth2AuthenticationException("Email not provided by " + registrationId);
//        }
//
//        String name = oAuth2User.getAttribute("name");
//        if (name == null) {
//            String given = oAuth2User.getAttribute("given_name");
//            String family = oAuth2User.getAttribute("family_name");
//            name = (given != null ? given : "") + " " + (family != null ? family : "");
//            name = name.trim();
//            if (name.isEmpty()) name = email.split("@")[0];
//        }
//
//        String picture = oAuth2User.getAttribute("picture");
//        String providerId = oAuth2User.getAttribute("sub"); // works for Google & LinkedIn
//
//        String finalName = name;
//        User user = userRepository.findByEmail(email)
//                .orElseGet(() -> {
//                    User u = new User();
//                    u.setEmail(email);
//                    u.setName(finalName);
//                    u.setPicture(picture);
//                    u.setProvider(registrationId.toUpperCase());
//                    u.setProviderId(providerId);
//                    return userRepository.save(u);
//                });
//
//        // Update in case name/picture changed
//        user.setName(name);
//        user.setPicture(picture);
//        user.setProviderId(providerId);
//        userRepository.save(user);
//
//        return new DefaultOAuth2User(
//                List.of(new SimpleGrantedAuthority("ROLE_USER")),
//                oAuth2User.getAttributes(),
//                "sub" // LinkedIn & Google both use "sub"
//        );
//    }
//}
//
