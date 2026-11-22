package com.universal.erp.platform.security;

import com.universal.erp.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Runs in its own transaction so failed-attempt counters survive the login exception. */
@Service
public class LoginAttemptService {
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        userRepository.findById(userId)
                .ifPresent(u -> u.registerFailedLogin(MAX_FAILED_ATTEMPTS, LOCK_DURATION, Instant.now()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> u.registerSuccessfulLogin(Instant.now()));
    }
}