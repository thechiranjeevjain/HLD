package com.example.capstone.auth.user;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse byEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }
}
