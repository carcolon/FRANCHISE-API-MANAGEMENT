package com.franchise.api.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "franchise.security")
public class FranchiseSecurityProperties {

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        /**
         * Secret key used to sign JWT tokens. Override in production via environment variable.
         */
        private String secret = "change-me-in-production";

        /**
         * Expiration in milliseconds.
         */
        private long expirationMs = 3_600_000L; // 1 hour
    }
}
