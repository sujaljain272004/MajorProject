package com.chargeup.service;

import com.chargeup.entity.User;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
