package com.example.risk.pretrade;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.nio.charset.StandardCharsets;

@Configuration @EnableMethodSecurity
public class SecurityConfiguration {
    private static final byte[] KEY="interview-demo-key-change-me-32bytes".getBytes(StandardCharsets.UTF_8);
    @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/actuator/prometheus","/api/internal/runtime","/").permitAll().anyRequest().authenticated()).oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwtAuthenticationConverter()))).build();}
    JwtAuthenticationConverter jwtAuthenticationConverter(){var authorities=new JwtGrantedAuthoritiesConverter();authorities.setAuthoritiesClaimName("roles");authorities.setAuthorityPrefix("ROLE_");var converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(authorities);return converter;}
    @Bean JwtDecoder jwtDecoder(){SecretKey k=new SecretKeySpec(KEY,"HmacSHA256");return NimbusJwtDecoder.withSecretKey(k).build();}
    @Bean JwtEncoder jwtEncoder(){SecretKey k=new SecretKeySpec(KEY,"HmacSHA256");return new NimbusJwtEncoder(new ImmutableSecret<>(k));}
}
