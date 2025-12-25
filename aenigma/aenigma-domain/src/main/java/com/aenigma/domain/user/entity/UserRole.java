package com.aenigma.domain.user.entity;

/**
 * 사용자 역할 (권한)
 */
public enum UserRole {
    /**
     * 게스트 사용자 (기본값)
     */
    GUEST,

    /**
     * 정규 등록 사용자
     */
    USER,

    /**
     * 관리자
     */
    ADMIN
}
