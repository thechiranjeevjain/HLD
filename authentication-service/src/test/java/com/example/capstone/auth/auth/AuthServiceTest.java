package com.example.capstone.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.capstone.auth.security.JwtService;
import com.example.capstone.auth.user.Role;
import com.example.capstone.auth.user.UserAccount;
import com.example.capstone.auth.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes";

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, new JwtService(SECRET, 60));
    }

    @Test
    void registerNormalizesEmailAndIssuesToken() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                " User@Example.com ",
                "UserPass123!",
                " Capstone User "
        ));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().displayName()).isEqualTo("Capstone User");
        assertThat(response.user().role()).isEqualTo(Role.USER);
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserAccount user = new UserAccount(
                "user@example.com",
                "Capstone User",
                passwordEncoder.encode("CorrectPass123!"),
                Role.USER,
                null,
                null
        );

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void mockOAuth2CreatesUserWhenIdentityIsNew() {
        when(userRepository.findByProviderAndProviderSubject("github", "gh-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.mockOAuth2Login(new MockOAuth2LoginRequest(
                "GitHub",
                "gh-123",
                "oauth@example.com",
                "OAuth User"
        ));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().provider()).isEqualTo("github");
        assertThat(response.user().email()).isEqualTo("oauth@example.com");
    }
}
