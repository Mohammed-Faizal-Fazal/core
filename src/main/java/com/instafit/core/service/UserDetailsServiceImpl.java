package com.instafit.core.service;

import com.instafit.core.entity.User;
import com.instafit.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        logger.debug("Loading user by phone number: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    logger.error("User not found with phone number: {}", phoneNumber);
                    return new UsernameNotFoundException("User not found with phone number: " + phoneNumber);
                });

        if (!user.getActive()) {
            logger.error("User account is inactive: {}", phoneNumber);
            throw new UsernameNotFoundException("User account is inactive");
        }

        logger.debug("User found: {} with role: {}", user.getPhoneNumber(), user.getRole());

        return user;
    }
}