package com.example.fast_chat.service.security;

import com.example.fast_chat.model.User;
import com.example.fast_chat.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(email));

    // Local accounts have a password, OAuth2 accounts have null password
    Collection<? extends GrantedAuthority> authorities = List.of();

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword() != null ? user.getPassword() : "",
        authorities
    );
  }
}
