package com.aenigma.domain.user.service;

import com.aenigma.domain.common.exception.DomainErrorCode;
import com.aenigma.domain.common.exception.DomainException;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import com.aenigma.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User 도메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String GUEST_PREFIX = "GUEST_";
    private static final int USERNAME_RANDOM_LENGTH = 8;
    private static final int DISPLAY_TAG_LENGTH = 4;
    private static final int MAX_TAG_ATTEMPTS = 100;

    @Transactional
    public User createGuestUser(String nickname) {
        String username = generateUniqueUsername();
        String displayTag = generateUniqueDisplayTag(nickname);

        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .displayTag(displayTag)
                .role(UserRole.GUEST)
                .build();

        User savedUser = userRepository.save(user);
        log.info("게스트 사용자 생성: {} ({})", savedUser.getDisplayName(), savedUser.getUsername());

        return savedUser;
    }

    private String generateUniqueUsername() {
        String username;
        int attempts = 0;

        do {
            username = GUEST_PREFIX + generateRandomAlphanumeric(USERNAME_RANDOM_LENGTH);
            attempts++;

            if (attempts > MAX_TAG_ATTEMPTS) {
                throw new DomainException(DomainErrorCode.USERNAME_GENERATION_FAILED);
            }
        } while (userRepository.existsByUsername(username));

        return username;
    }

    private String generateUniqueDisplayTag(String nickname) {
        Set<String> usedTags = userRepository.findDisplayTagsByNickname(nickname)
                .stream()
                .collect(Collectors.toSet());

        String tag;
        int attempts = 0;

        do {
            tag = String.format("%04d", secureRandom.nextInt(10000));
            attempts++;

            if (attempts > MAX_TAG_ATTEMPTS) {
                throw new DomainException(DomainErrorCode.DISPLAYTAG_GENERATION_FAILED);
            }
        } while (usedTags.contains(tag));

        return tag;
    }

    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    public Optional<User> findByNicknameAndTag(String nickname, String displayTag) {
        return userRepository.findByNicknameAndDisplayTag(nickname, displayTag);
    }

    public List<User> searchByNickname(String keyword) {
        return userRepository.searchByNickname(keyword);
    }

    @Transactional
    public User login(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException(DomainErrorCode.USER_NOT_FOUND));

        user.updateLastLogin();
        log.info("사용자 로그인: {} ({})", user.getDisplayName(), user.getUsername());

        return user;
    }

    @Transactional
    public User changeNickname(UUID userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(DomainErrorCode.USER_NOT_FOUND));

        String newDisplayTag = generateUniqueDisplayTag(newNickname);
        user.changeNickname(newNickname, newDisplayTag);

        log.info("닉네임 변경: {} -> {}", user.getUsername(), user.getDisplayName());

        return user;
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(DomainErrorCode.USER_NOT_FOUND));

        user.deactivate();
        log.info("사용자 비활성화: {} ({})", user.getDisplayName(), user.getUsername());
    }
}
