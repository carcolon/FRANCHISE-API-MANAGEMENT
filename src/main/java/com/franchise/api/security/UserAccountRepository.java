package com.franchise.api.security;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByPasswordResetToken(String token);

    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
