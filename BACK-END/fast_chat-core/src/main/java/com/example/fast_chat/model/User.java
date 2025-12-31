package com.example.fast_chat.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Entity
//@Table(name = "app_user")
@Table(name = "oauth2_users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String email;
    private String provider;
    private String providerId;
    private String password;
    private String picture;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    List<UserRole> roles;

}
