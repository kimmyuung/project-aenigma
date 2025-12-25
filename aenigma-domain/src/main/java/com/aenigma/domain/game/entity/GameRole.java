package com.aenigma.domain.game.entity;

/**
 * 게임 내 역할
 */
public enum GameRole {
    /**
     * 범인 - 시민을 제거하는 역할
     */
    /**
     * 범인 - 살인 사건의 진범
     */
    CRIMINAL("범인", "정체를 들키지 않도록 행동하세요."),

    /**
     * 탐정 - 사건을 수사하는 탐정
     */
    DETECTIVE("탐정", "단서를 모아 범인을 찾아내세요."),

    /**
     * 용의자 - 사건의 용의자로 지목된 사람
     */
    SUSPECT("용의자", "자신의 결백을 증명하고 범인을 찾으세요.");

    private final String displayName;
    private final String description;

    GameRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 마피아(범인) 팀인지 확인
     */
    /**
     * 범인 팀인지 확인
     */
    public boolean isCriminalTeam() {
        return this == CRIMINAL;
    }

    /**
     * 시민(탐정/용의자) 팀인지 확인
     */
    public boolean isCitizenTeam() {
        return this != CRIMINAL;
    }
}
