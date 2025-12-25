package com.aenigma.domain.user.repository;

import com.aenigma.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User 엔티티 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByNickname(String nickname);

    Optional<User> findByNicknameAndDisplayTag(String nickname, String displayTag);

    long countByNickname(String nickname);

    @Query("SELECT u.displayTag FROM User u WHERE u.nickname = :nickname AND u.displayTag IS NOT NULL")
    List<String> findDisplayTagsByNickname(@Param("nickname") String nickname);

    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.nickname LIKE %:keyword% AND u.isActive = true")
    List<User> searchByNickname(@Param("keyword") String keyword);
}
