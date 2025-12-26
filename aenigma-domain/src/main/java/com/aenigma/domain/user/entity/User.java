package com.aenigma.domain.user.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 엔티티
 * 
 * 게스트 로그인 중심 설계:
 * - username: 시스템 생성 (GUEST_xxxxxxxx)
 * - nickname: 사용자 입력 (중복 허용)
 * - displayTag: 닉네임 구분용 태그 (#1234)
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_nickname", columnList = "nickname")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 시스템 생성 고유 사용자명 (GUEST_xxxxxxxx)
     * 로그인 식별자로 사용
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 사용자 입력 닉네임 (중복 허용)
     * 화면에 표시되는 이름
     */
    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    /**
     * 닉네임 구분용 태그 (4자리 숫자)
     * 화면 표시: nickname#displayTag (예: 탐정 김명호#1234)
     */
    @Column(name = "display_tag", length = 4)
    private String displayTag;

    /**
     * 사용자 역할
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.GUEST;

    /**
     * 마지막 로그인 일시
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 계정 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Discord 사용자 ID (연동 시 설정)
     */
    @Column(name = "discord_id", length = 30)
    private String discordId;

    // === Business Methods ===

    /**
     * 화면 표시용 전체 닉네임 반환
     */
    public String getDisplayName() {
        if (displayTag == null || displayTag.isBlank()) {
            return nickname;
        }
        return nickname + "#" + displayTag;
    }

    /**
     * 로그인 시간 갱신
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 닉네임 변경
     */
    public void changeNickname(String newNickname, String newDisplayTag) {
        this.nickname = newNickname;
        this.displayTag = newDisplayTag;
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 계정 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}
