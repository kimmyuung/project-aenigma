# Aenigma ERD (Entity Relationship Diagram)

```mermaid
erDiagram
    USERS {
        UUID id PK
        String username
        String nickname
        LocalDateTime created_at
        LocalDateTime updated_at
    }

    ROOMS {
        UUID id PK
        String room_code
        String title
        Boolean is_private
        String password
        RoomState state
        LocalDateTime created_at
        LocalDateTime updated_at
    }

    GAMES {
        UUID id PK
        Integer round_number
        GamePhase phase
        Integer day_count
        LocalDateTime started_at
        LocalDateTime finished_at
        GameRole winner_team
        LocalDateTime created_at
        LocalDateTime updated_at
        UUID room_id FK
    }

    GAME_PLAYERS {
        UUID id PK
        GameRole role
        Boolean is_alive
        LocalDateTime created_at
        LocalDateTime updated_at
        UUID game_id FK
        UUID user_id FK
    }

    CHAT_MESSAGES {
        Long id PK
        String content
        MessageType type
        LocalDateTime created_at
        UUID game_id FK
        UUID sender_id FK
    }

    VOTES {
        Long id PK
        UUID target_player_id
        LocalDateTime created_at
        UUID game_id FK
        UUID voter_id FK
    }

    %% Relationships
    ROOMS ||--o{ GAMES : "has"
    GAMES ||--o{ GAME_PLAYERS : "includes"
    USERS ||--o{ GAME_PLAYERS : "participates_as"
    
    GAMES ||--o{ CHAT_MESSAGES : "contains"
    USERS ||--o{ CHAT_MESSAGES : "sends"

    GAMES ||--o{ VOTES : "has"
    GAME_PLAYERS ||--o{ VOTES : "casts (voter)"
    GAME_PLAYERS ||--o| VOTES : "targets"
```

## Description
- **USERS**: 플랫폼 가입 사용자
- **ROOMS**: 게임 대기방 (게임 시작 전)
- **GAMES**: 실제 진행되는 게임 세션 (Room 1 : N Game)
- **GAME_PLAYERS**: 특정 게임에 참여한 사용자 정보 (역할, 생존여부 등)
- **CHAT_MESSAGES**: 게임 내 채팅 로그
- **VOTES**: 최종 투표 기록
