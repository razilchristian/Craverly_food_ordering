package com.cravely.backend.repository;

import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByRole(Role role);
    long countByActive(Boolean active);
    long countByRoleAndActive(Role role, Boolean active);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<User> findTop10ByOrderByCreatedAtDesc();
}

