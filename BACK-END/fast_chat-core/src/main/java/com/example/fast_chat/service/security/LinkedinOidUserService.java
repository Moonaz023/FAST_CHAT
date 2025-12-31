package com.example.fast_chat.service.security;

import com.example.fast_chat.model.Role;
import com.example.fast_chat.model.User;
import com.example.fast_chat.model.UserRole;
import com.example.fast_chat.repository.RoleRepository;
import com.example.fast_chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class LinkedinOidUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    UserRepository userRepository;
    RoleRepository roleRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        final OidcUserService delegate = new OidcUserService();
        OidcUser oidcUser = delegate.loadUser(userRequest);
        List<GrantedAuthority> mappedAuthorities = new ArrayList<>(oidcUser.getAuthorities());

        String email = Objects.requireNonNull(oidcUser.getAttribute("email"));

        var user = userRepository.findByEmail(email)
            .orElseGet(() -> createNewUser(oidcUser, userRequest));

        user.getRoles().forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority(role.getRole().getName())));

        return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private User createNewUser(OidcUser oidcUser, OidcUserRequest userRequest) {
        String name = Objects.requireNonNull(oidcUser.getAttribute("name"));
        String email = Objects.requireNonNull(oidcUser.getAttribute("email"));
        String picture = oidcUser.getAttribute("picture");
        String providerId = oidcUser.getName();
        String provider = userRequest.getClientRegistration().getRegistrationId();

        var role = roleRepository.findByName(Role.ROLE_USER)
            .orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(Role.ROLE_USER);
                return roleRepository.save(newRole);
            });

        var user = User.builder()
            .name(name)
            .email(email)
            .picture(picture)
            .providerId(providerId)
            .provider(provider)
            .build();

        user.setRoles(List.of(UserRole.builder().user(user).role(role).build()));

        return userRepository.save(user);
    }
}