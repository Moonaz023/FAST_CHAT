package com.example.fast_chat.repository;

import com.example.fast_chat.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u " +
        "WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<User> searchByNameOrEmail(@Param("q") String query);
}
