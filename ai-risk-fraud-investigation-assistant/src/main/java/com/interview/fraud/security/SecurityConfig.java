package com.interview.fraud.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("analyst").password("{noop}analyst-demo").roles("ANALYST").build(),
                User.withUsername("senior").password("{noop}senior-demo").roles("SENIOR_ANALYST").build(),
                User.withUsername("admin").password("{noop}admin-demo").roles("ADMIN").build(),
                User.withUsername("auditor").password("{noop}auditor-demo").roles("AUDITOR").build());
    }
    @Bean SecurityFilterChain filter(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/", "/assets/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/cases/*/approve").hasAnyRole("SENIOR_ANALYST", "ADMIN")
                .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
                .httpBasic(basic -> {}).build();
    }
}
