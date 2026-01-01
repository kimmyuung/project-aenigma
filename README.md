# 🎭 AENIGMA - 온라인 머더미스터리 게임

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()
[![Version](https://img.shields.io/badge/version-0.1.0--MVP-orange)]()

> 친구들과 함께 즐기는 온라인 추리 게임. 범인을 찾아내거나, 끝까지 숨어라!

**마지막 업데이트**: 2026-01-01

## 📦 프로젝트 구조

```
aenigma/
├── aenigma-api/          # REST API 서버 (Spring Boot)
├── aenigma-domain/       # 도메인 모델, 서비스, 레포지토리
├── aenigma-socket/       # WebSocket + Discord 봇
├── aenigma-ai/           # AI 모듈 (개발 예정)
├── aenigma-front/        # React 프론트엔드
├── aenigma-mobile/       # Expo 모바일 앱 (개발 중)
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

### 모바일 앱 실행
```bash
cd aenigma-mobile
npm install
npx expo start
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
- ✅ 게임 결과 화면 (투표 결과, 역할 공개, 사건 전말)

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

### Mobile
- **Framework**: Expo (React Native)
- **Navigation**: Expo Router
- **State**: React Context

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

## 📊 프로젝트 완성도 (2026-01-01 기준)

| 모듈 | 완성도 | 상태 | 비고 |
|------|--------|------|------|
| aenigma-domain | 90% | 🟢 | 엔티티, 서비스, 레포지토리 완료 |
| aenigma-api | 85% | 🟢 | 전체 API 엔드포인트 구현 |
| aenigma-socket | 90% | 🟢 | WebSocket + Discord 완료 |
| aenigma-front | 70% | 🟡 | 핵심 UI 완료, 최적화 필요 |
| aenigma-mobile | 50% | 🟡 | 기본 화면 구현, 게임 결과 미완 |
| aenigma-ai | 30% | 🔴 | 기본 구조만 존재 |

**전체: ~75% (MVP 수준 달성)**

### ✅ 완료된 핵심 기능
- 회원 인증 (가입/로그인/JWT)
- 방 생성, 입장, 대기실
- 게임 진행 (역할 배정, 라운드, 페이즈)
- 단서 시스템 (공개, 수집)
- 투표 시스템 (비밀 투표, 결과 집계)
- 실시간 채팅 (WebSocket + 귓속말)
- 게임 결과 (투표 결과, 역할 공개, 사건 전말)
- Discord GM 명령어 (16개)

## 📁 API 엔드포인트

### Auth
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인

### Rooms
- `GET /api/rooms` - 방 목록 (검색/필터 지원)
- `POST /api/rooms` - 방 생성
- `POST /api/rooms/{id}/join` - 입장
- `POST /api/rooms/{id}/leave` - 퇴장

### Games
- `POST /api/games` - 게임 생성
- `GET /api/games/{id}` - 게임 조회
- `GET /api/games/{id}/clues` - 단서 목록
- `GET /api/games/{id}/my-role` - 내 역할
- `POST /api/games/{id}/vote` - 투표
- `GET /api/games/{id}/result` - 게임 결과

### Scenarios
- `POST /api/scenarios/upload` - 시나리오 업로드

## 🧪 테스트

### 백엔드 테스트 (26개)
- **Entity 테스트**: Game, User, Room, Vote, ChatMessage, GamePlayer, Scenario, Round 등 (11개)
- **Service 테스트**: GameService, VoteService, UserService, RoomService, ChatService 등 (6개)
- **Controller 테스트**: AuthController, RoomController, GameController, VoteController, ChatController (6개)
- **Parser 테스트**: MarkdownScenarioParser (2개)
- **Config 테스트**: SecurityConfig (1개)

### 프론트엔드 테스트 (7개)
- HomePage 테스트
- AuthPages 테스트
- RoomsPage 테스트
- RoomDetailPage 테스트
- GamePage 테스트
- CluePanel 테스트
- VotePanel 테스트

## 🚧 향후 개발 예정

1. **모바일 앱 완성** - 게임 결과 화면, 푸시 알림
2. **AI 기능 개발** - 게임 데이터 분석, 시나리오 추천
3. **성능 최적화** - Redis 캐싱, React 최적화
4. **배포** - Docker Compose, 클라우드 배포

## 📄 라이선스

MIT License

## 👥 기여

이슈 및 PR 환영합니다!
