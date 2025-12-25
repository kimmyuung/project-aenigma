# 🕵️ Aenigma (애니그마) - Online Murder Mystery Platform

**Aenigma**는 실시간 상호작용과 AI 기술이 결합된 **차세대 온라인 머더 미스터리 플랫폼**입니다.
오프라인 보드게임의 몰입감을 웹/앱 환경으로 확장하며, AI 기술을 활용해 더욱 풍부한 스토리텔링과 추리 경험을 제공합니다.

---

## 🚀 Key Features (핵심 기능)

이 프로젝트는 다음 3가지 핵심 축을 중심으로 개발되고 있습니다.

### 1. 🌐 Real-time Online Murder Mystery (Backend Core)
웹소켓(WebSocket) 기반의 실시간 게임 엔진으로, 플레이어들이 동시에 접속하여 추리를 진행합니다.
- **실시간 동기화**: `Intro` -> `Investigation` -> `Final Vote` -> `Conclusion`으로 이어지는 게임 페이즈와 타이머가 모든 클라이언트에서 오차 없이 동기화됩니다.
- **실시간 채팅 & 투표**: `Chat` 도메인과 `Vote` 도메인을 통해 밀담(Whisper), 전체 채팅, 그리고 긴장감 넘치는 비공개 투표를 지원합니다.
- **보안 로직**: 자신의 역할(`Detectived`, `Criminal`, `Suspect`)에 따라 조회 가능한 단서와 정보가 엄격하게 분리됩니다.

### 2. 🤖 AI Interrogation System (AI & LLM)
정해진 스크립트를 읽는 NPC가 아니라, 생성형 AI(LLM)가 연기하는 용의자를 직접 심문할 수 있습니다.
- **AI 페르소나**: 각 용의자에게 성격, 비밀, 알리바이가 프롬프트로 주입되어 있어, 플레이어의 자유로운 질문에 맞춰 반응합니다.
- **범인의 거짓말**: 범인 역할의 AI는 알리바이를 조작하거나 거짓말을 하도록 설계되어 있어 추리의 난이도를 높입니다.
- **Game Master AI**: 게임의 진행 상황을 요약하거나, 교착 상태에서 미묘한 힌트를 던져주는 AI 사회자가 존재합니다.

### 3. 📱 Companion App (Frontend & Mobile)
오프라인에서 친구들과 머더 미스터리를 즐길 때, 사회자 역할을 대신해주는 컴패니언 앱입니다.
- **Local-First**: React Native 기반으로 iOS/Android를 지원하며, 개인별 모바일 기기에서 자신의 비밀 단서를 안전하게 확인합니다.
- **자동 진행**: 타이머 종료 시 자동으로 다음 단계로 넘어가고, 분위기에 맞는 BGM을 재생하여 몰입감을 극대화합니다.

---

## 🛠️ Tech Stack

### Backend (Server)
- **Language**: Java 17
- **Framework**: Spring Boot 3.x (Multi-module Architecture)
- **Database**: H2 (Dev) / MySQL (Prod), JPA (Hibernate), QueryDSL
- **Real-time**: WebSocket (STOMP)
- **Build**: Gradle (Kotlin DSL)

### Frontend (Client)
- **Mobile**: React Native (Expo)
- **Web**: React.js (Planned)
- **Style**: TailwindCSS (Planned)

### AI & Data
- **Model**: OpenAI GPT-4 / Gemini Pro (via API)
- **Technique**: Prompt Engineering, RAG (Retrieval-Augmented Generation)

---

## 📂 Project Structure

Aenigma는 확장성과 유지보수성을 고려하여 **멀티 모듈 아키텍처**로 설계되었습니다.

```bash
aenigma/
├── aenigma-api/        # REST API 및 WebSocket 엔드포인트 (Presentation Layer)
├── aenigma-domain/     # 핵심나 비즈니스 로직 및 엔티티 (Domain Layer)
│   ├── game/           # 게임 진행, 페이즈, 룸 관리
│   ├── chat/           # 채팅 메시지 처리
│   ├── vote/           # 투표 시스템
│   ├── user/           # 사용자 및 인증
│   └── common/         # 공통 유틸리티
├── aenigma-mobile/     # React Native 클라이언트 프로젝트
└── ...
```

---

## 📝 Roadmap

- [x] **Phase 1: Domain Modeling**
  - [x] 멀티 모듈 구조 설계 및 도메인 분리 (`game`, `chat`, `vote` 등)
  - [x] 머더 미스터리 전용 게임 로직 구현 (`GamePhase`, `GameRole`)
  - [x] 마피아 게임 잔재 제거 및 장르 최적화

- [ ] **Phase 2: Core Features**
  - [ ] WebSocket 기반 실시간 채팅 및 페이즈 동기화 구현
  - [ ] 모바일 앱(React Native) 연동 및 테스트
  - [ ] JWT 기반 인증 시스템 고도화

- [ ] **Phase 3: AI Integration**
  - [ ] LLM API 연동 및 용의자 페르소나 프롬프트 설계
  - [ ] AI 심문 프로토타입 구현

---

## 🤝 Contribution

이 프로젝트는 개인 포트폴리오 및 기술적 챌린지를 위한 프로젝트입니다.
함께 고민하고 싶은 기술적 주제(동시성, AI 연동 등)가 있다면 언제든 Issue나 PR을 환영합니다.
