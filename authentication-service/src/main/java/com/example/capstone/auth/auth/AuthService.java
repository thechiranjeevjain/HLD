package com.example.capstone.auth.auth;

import com.example.capstone.auth.security.JwtService;
import com.example.capstone.auth.user.Role;
import com.example.capstone.auth.user.UserAccount;
import com.example.capstone.auth.user.UserRepository;
import com.example.capstone.auth.user.UserResponse;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        UserAccount user = new UserAccount(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                Role.USER,
                null,
                null
        );
        return issueToken(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!user.isEnabled() || !StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        return issueToken(user);
    }

    public AuthResponse mockOAuth2Login(MockOAuth2LoginRequest request) {
        String provider = request.provider().trim().toLowerCase(Locale.ROOT);
        String providerSubject = request.providerSubject().trim();
        String email = normalizeEmail(request.email());

        UserAccount user = userRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseGet(() -> userRepository.save(new UserAccount(
                        email,
                        request.displayName().trim(),
                        null,
                        Role.USER,
                        provider,
                        providerSubject
                )));

        return issueToken(user);
    }

    private AuthResponse issueToken(UserAccount user) {
        String token = jwtService.createToken(user);
        return new AuthResponse("Bearer", token, jwtService.ttlSeconds(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
