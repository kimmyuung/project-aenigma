# 🎭 AENIGMA - 온라인 머더미스터리 게임

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

> 친구들과 함께 즐기는 온라인 추리 게임. 범인을 찾아내거나, 끝까지 숨어라!

## 📦 프로젝트 구조

```
aenigma/
├── aenigma-api/          # REST API 서버 (Spring Boot)
├── aenigma-domain/       # 도메인 모델, 서비스, 레포지토리
├── aenigma-socket/       # WebSocket + Discord 봇
├── aenigma-ai/           # AI 모듈 (게임 데이터 수집)
├── aenigma-front/        # React 프론트엔드
├── aenigma-mobile/       # 모바일 앱 (예정)
└── aenigma-common/       # 공통 유틸리티
```

## 🚀 시작하기

### 사전 요구사항
- Java 17+
- Node.js 18+
- PostgreSQL
- Discord Bot Token (선택)

### 백엔드 실행
```bash
# Gradle 빌드
./gradlew build

# API 서버 실행
./gradlew :aenigma-api:bootRun

# Socket 서버 실행
./gradlew :aenigma-socket:bootRun
```

### 프론트엔드 실행
```bash
cd aenigma-front
npm install
npm run dev
```

### 테스트 실행
```bash
# 백엔드 테스트
./gradlew test

# 프론트엔드 테스트
cd aenigma-front
npm test
```

## 🎮 주요 기능

### 게임 시스템
- ✅ 역할 배정 (탐정, 범인, 시민)
- ✅ 다중 조사 라운드 (1~3회)
- ✅ 비밀 투표 시스템
- ✅ 단서 수집 및 공개
- ✅ 실시간 채팅 (WebSocket)

### Discord 연동 (GM 명령어)

| 카테고리 | 명령어 |
|----------|--------|
| **게임** | `/game start\|end\|status`, `/phase next\|info` |
| **설정** | `/timer`, `/announce`, `/config` |
| **투표** | `/vote all\|status\|result` |
| **플레이어** | `/players list\|reveal\|kick` |
| **단서** | `/clue list\|reveal\|give` |
| **진행** | `/secret`, `/whisper`, `/mute`, `/unmute` |
| **결과** | `/result reveal`, `/summary` |
| **제어** | `/pause`, `/resume` |

### 시나리오 시스템
- ✅ 마크다운 시나리오 업로드
- ✅ 역할 및 단서 자동 파싱
- ✅ 게임-시나리오 연동

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **ORM**: JPA / QueryDSL
- **Security**: JWT
- **WebSocket**: STOMP
- **Discord**: JDA 5.x

### Frontend
- **Framework**: React 19 + TypeScript
- **Build**: Vite
- **State**: React Context
- **Testing**: Vitest + Testing Library

### Database
- PostgreSQL
- Redis (캐싱, 예정)

## 🎧 권장 플레이 방식

최고의 게임 경험을 위해 **웹 + Discord 음성**을 함께 사용하세요!

### 플레이어 세팅
```
🖥️ 웹 화면 (메인)  → 역할, 단서, 투표 확인
🎧 Discord 음성    → 대화, 밀담, 추리
```

### 왜 음성 채팅이 중요한가요?

| 요소 | 텍스트 채팅 | 음성 채팅 |
|------|-----------|----------|
| **거짓말 탐지** | ❌ 표정/목소리 못 읽음 | ✅ 떨리는 목소리, 망설임 포착 |
| **심리전** | ❌ 느린 반응 | ✅ 즉각적인 반응 |
| **밀담 긴장감** | ❌ 누가 DM 중인지 모름 | ✅ "저 둘 밀담실 갔다!" |
| **몰입감** | ⚠️ 제한적 | ✅ 실제 대화하는 느낌 |

### 음성 채널 구성
- 🎮 **Lobby** - 전체 토론
- 🤫 **Whisper** - 1:1 밀담
- ❓ **GM문의** - GM과 비공개 대화

## 📊 프로젝트 완성도

| 모듈 | 완성도 | 상태 |
|------|--------|------|
| aenigma-domain | 85% | 🟢 |
| aenigma-api | 80% | 🟢 |
| aenigma-socket | 90% | 🟢 |
| aenigma-front | 60% | 🟡 |
| aenigma-ai | 40% | 🟡 |

**전체: ~70%**

## 📁 API 엔드포인트

### Auth
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인

### Rooms
- `GET /api/rooms` - 방 목록
- `POST /api/rooms` - 방 생성
- `POST /api/rooms/{id}/join` - 입장

### Games
- `POST /api/games` - 게임 생성
- `GET /api/games/{id}` - 게임 조회
- `GET /api/games/{id}/clues` - 단서 목록
- `GET /api/games/{id}/my-role` - 내 역할
- `POST /api/games/{id}/vote` - 투표

### Scenarios
- `POST /api/scenarios/upload` - 시나리오 업로드

## 🧪 테스트

### 백엔드 테스트 (17개)
- Entity 테스트: Game, User, Room, Vote, ChatMessage
- Service 테스트: GameService, VoteService, UserService, RoomService, ChatService
- Controller 테스트: AuthController, RoomController
- Parser 테스트: MarkdownScenarioParser

### 프론트엔드 테스트
- HomePage 테스트 (5개)
- AuthPages 테스트 (7개)

## 📄 라이선스

MIT License

## 👥 기여

이슈 및 PR 환영합니다!
