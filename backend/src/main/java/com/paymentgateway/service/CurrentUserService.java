package com.paymentgateway.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.paymentgateway.entity.User;
import com.paymentgateway.exception.ResourceNotFoundException;
import com.paymentgateway.exception.UnauthorizedException;
import com.paymentgateway.repository.UserRepository;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {

            throw new UnauthorizedException(
                    "User is not authenticated"
            );
        }

        String email =
                authentication
                        .getPrincipal()
                        .toString();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}

