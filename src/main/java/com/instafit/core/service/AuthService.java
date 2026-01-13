package com.instafit.core.service;

import com.instafit.core.entity.User;
import com.instafit.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getCreatedBy() == null) {
            user.setCreatedBy("SELF_REGISTER");
        }
        if (user.getUpdatedBy() == null) {
            user.setUpdatedBy("SELF_REGISTER");
        }

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {} with role {}", savedUser.getPhoneNumber(), savedUser.getRole());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public boolean isPhoneNumberAvailable(String phoneNumber) {
        return !userRepository.existsByPhoneNumber(phoneNumber);
    }
}