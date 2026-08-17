package com.example.documents.security;

import java.util.Set;

public record AuthenticatedUser(String subject, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... candidateRoles) {
        for (String role : candidateRoles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
