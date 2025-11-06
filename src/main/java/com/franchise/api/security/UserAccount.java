package com.franchise.api.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    private String fullName;

    private String email;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Set<Role> roles = Set.of(Role.USER);

    private String passwordResetToken;

    private Instant passwordResetExpiration;

    @Builder.Default
    private boolean passwordChangeRequired = false;
}
