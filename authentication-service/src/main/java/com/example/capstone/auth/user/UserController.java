package com.example.capstone.auth.user;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        return userService.byEmail(authentication.getName());
    }

    @GetMapping("/api/admin/users")
    public List<UserResponse> users() {
        return userService.listUsers();
    }
}
