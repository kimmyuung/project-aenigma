package com.aenigma.ai.assistant.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 내레이션 생성 서비스
 * 
 * 게임 진행에 필요한 내레이션과 페이즈 전환 문구를 생성합니다.
 * 현재는 템플릿 기반이며, LLM 연동 시 동적 생성으로 확장 가능합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NarrationService {

    /**
     * 페이즈 전환 내레이션 생성
     */
    public String generatePhaseTransition(PhaseTransitionContext context) {
        log.debug("페이즈 전환 내레이션 생성: {} -> {}", context.getFromPhase(), context.getToPhase());

        return switch (context.getToPhase()) {
            case "INTRO" -> generateIntroNarration(context);
            case "LOBBY" -> generateLobbyNarration(context);
            case "INVESTIGATION" -> generateInvestigationNarration(context);
            case "FINAL_VOTE" -> generateFinalVoteNarration(context);
            case "CONCLUSION" -> generateConclusionNarration(context);
            case "FINISHED" -> generateFinishNarration(context);
            default -> "다음 단계로 진행합니다.";
        };
    }

    /**
     * 게임 시작 내레이션
     */
    public String generateGameStartNarration(String scenarioTitle, int playerCount) {
        return String.format("""
                🎭 **%s**

                %d명의 플레이어가 모였습니다.

                곧 각자의 역할이 배정됩니다.
                자신의 역할을 확인하고, 게임에 임해주세요.

                범인은 끝까지 의심을 피해야 하고,
                탐정과 시민들은 범인을 찾아내야 합니다.

                행운을 빕니다! 🔍
                """, scenarioTitle, playerCount);
    }

    /**
     * 역할 배정 안내
     */
    public String generateRoleAssignmentNotice() {
        return """
                📜 **역할이 배정되었습니다!**

                각자에게 비밀 역할이 전달되었습니다.
                자신의 역할을 확인해주세요.

                ⚠️ 주의: 역할은 비밀입니다. 다른 사람에게 직접적으로 밝히지 마세요.
                """;
    }

    private String generateIntroNarration(PhaseTransitionContext context) {
        return """
                📖 **이야기가 시작됩니다...**

                GM의 안내에 따라 배경 설명을 들어주세요.
                이야기 속 인물이 되어, 추리극에 빠져보세요.
                """;
    }

    private String generateLobbyNarration(PhaseTransitionContext context) {
        return """
                🏛️ **로비 단계**

                모든 참가자가 모입니다.
                서로 인사를 나누고, 게임 시작을 준비해주세요.
                """;
    }

    private String generateInvestigationNarration(PhaseTransitionContext context) {
        int round = context.getRound();

        if (round == 1) {
            return """
                    🔍 **첫 번째 조사 시간**

                    사건이 발생했습니다!
                    참가자들은 자유롭게 대화하며 단서를 수집하세요.

                    💬 공개 채팅으로 질문하고, 귓속말로 비밀 대화를 나눌 수 있습니다.

                    의심스러운 사람을 관찰하세요. 진실은 언제나 작은 단서에 숨어 있습니다.
                    """;
        } else if (round == 2) {
            return String.format("""
                    🔍 **두 번째 조사 시간**

                    조사가 계속됩니다.
                    첫 번째 조사에서 발견한 단서를 바탕으로 추리를 이어가세요.

                    ⏰ 시간이 많지 않습니다. 결정적인 증거를 찾아야 합니다.
                    """);
        } else {
            return String.format("""
                    🔍 **%d번째 조사 시간**

                    마지막 기회입니다!
                    지금까지의 모든 정보를 종합하여 범인을 추리하세요.

                    ⚠️ 이 조사가 끝나면 최종 투표가 진행됩니다.
                    """, round);
        }
    }

    private String generateFinalVoteNarration(PhaseTransitionContext context) {
        return """
                ⚖️ **최종 투표 시간**

                조사가 모두 끝났습니다.
                이제 범인이라고 생각하는 사람에게 투표해주세요.

                🗳️ 가장 많은 표를 받은 사람이 범인으로 지목됩니다.

                신중하게 결정하세요. 잘못된 판단은 범인의 승리로 이어집니다!
                """;
    }

    private String generateConclusionNarration(PhaseTransitionContext context) {
        return """
                🎬 **결과 발표**

                투표가 완료되었습니다.
                GM이 결과를 발표합니다.

                과연 진짜 범인을 찾아냈을까요...?
                """;
    }

    private String generateFinishNarration(PhaseTransitionContext context) {
        return """
                🏆 **게임 종료**

                추리극이 막을 내립니다.
                모든 참가자 여러분, 수고하셨습니다!

                각자의 역할을 공개하고, 게임을 회고해보세요.
                다음 게임에서 또 만나요! 🎭
                """;
    }

    /**
     * 페이즈 전환 컨텍스트
     */
    @lombok.Builder
    @Getter
    public static class PhaseTransitionContext {
        private String fromPhase;
        private String toPhase;
        private int round;
        private int eliminatedCount;
        private Map<String, Object> extra;
    }
}
