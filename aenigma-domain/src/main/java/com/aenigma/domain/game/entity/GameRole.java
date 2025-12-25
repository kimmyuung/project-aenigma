package com.aenigma.domain.game.entity;

/**
 * 게임 내 역할
 */
public enum GameRole {
    /**
     * 범인 - 시민을 제거하는 역할
     */
    KILLER("범인", "밤에 한 명을 제거할 수 있습니다."),

    /**
     * 탐정 - 플레이어의 역할을 조사하는 역할
     */
    DETECTIVE("탐정", "밤에 한 명의 역할을 확인할 수 있습니다."),

    /**
     * 시민 - 투표로 범인을 찾아내는 역할
     */
    CITIZEN("시민", "투표를 통해 범인을 찾아내세요."),

    /**
     * 의사 - 플레이어를 보호하는 역할
     */
    DOCTOR("의사", "밤에 한 명을 범인으로부터 보호할 수 있습니다."),

    /**
     * 경찰 - 체포 능력을 가진 역할
     */
    POLICE("경찰", "특정 조건에서 용의자를 즉시 체포할 수 있습니다.");

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
    public boolean isKillerTeam() {
        return this == KILLER;
    }

    /**
     * 시민 팀인지 확인
     */
    public boolean isCitizenTeam() {
        return this != KILLER;
    }
}
