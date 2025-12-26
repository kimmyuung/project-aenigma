package com.aenigma.ai.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시나리오 추천 서비스
 * 
 * 로비에서 인원 수 기반으로 적절한 시나리오를 추천합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioRecommendService {

    /**
     * 인원 수에 맞는 시나리오 추천
     */
    public ScenarioRecommendation recommend(int playerCount) {
        log.info("시나리오 추천 요청: playerCount={}", playerCount);

        return switch (playerCount) {
            case 4 -> getSmallGroupScenario();
            case 5, 6 -> getMediumGroupScenario();
            case 7, 8 -> getLargeGroupScenario();
            default -> {
                if (playerCount < 4) {
                    yield getMinimalScenario();
                } else {
                    yield getMaxGroupScenario();
                }
            }
        };
    }

    private ScenarioRecommendation getMinimalScenario() {
        return ScenarioRecommendation.builder()
                .title("미니 추리극: 밀실의 비밀")
                .description("소규모 인원에 적합한 간단한 추리 시나리오입니다.")
                .recommendedPlayers("3~4명")
                .duration("30분~1시간")
                .difficulty("입문")
                .roles(new String[] { "탐정", "범인", "목격자" })
                .synopsis("작은 도서관에서 발생한 도난 사건. 범인은 이 중에 있다!")
                .build();
    }

    private ScenarioRecommendation getSmallGroupScenario() {
        return ScenarioRecommendation.builder()
                .title("저주받은 저택의 밤")
                .description("4명이 진행하기 좋은 클래식 추리 시나리오입니다.")
                .recommendedPlayers("4명")
                .duration("1~1.5시간")
                .difficulty("초급")
                .roles(new String[] { "탐정", "범인", "의사", "용의자" })
                .synopsis("폭풍우 치는 밤, 고풍스러운 저택에서 주인이 숨진 채 발견되었다. " +
                        "손님들 중 한 명이 범인이다!")
                .build();
    }

    private ScenarioRecommendation getMediumGroupScenario() {
        return ScenarioRecommendation.builder()
                .title("열차 살인 사건")
                .description("5~6명에 최적화된 본격 추리 시나리오입니다.")
                .recommendedPlayers("5~6명")
                .duration("1.5~2시간")
                .difficulty("중급")
                .roles(new String[] { "탐정", "범인", "공범", "피해자 친구", "승무원", "수상한 승객" })
                .synopsis("동양 급행열차를 타고 가던 중, 부유한 사업가가 살해되었다. " +
                        "눈보라로 열차는 고립되었고, 범인은 반드시 이 열차 안에 있다!")
                .build();
    }

    private ScenarioRecommendation getLargeGroupScenario() {
        return ScenarioRecommendation.builder()
                .title("가면무도회의 비극")
                .description("대규모 인원에 적합한 복잡한 추리 시나리오입니다.")
                .recommendedPlayers("7~8명")
                .duration("2~2.5시간")
                .difficulty("상급")
                .roles(new String[] { "탐정", "범인", "공범", "백작", "백작부인",
                        "하인", "경호원", "손님" })
                .synopsis("화려한 가면무도회가 열린 밤, 무도회장 한가운데서 비명이 울린다. " +
                        "가면 뒤에 숨은 살인마를 찾아라!")
                .build();
    }

    private ScenarioRecommendation getMaxGroupScenario() {
        return ScenarioRecommendation.builder()
                .title("그랑 호텔 미스터리")
                .description("대규모 파티에 적합한 그룹 추리 시나리오입니다.")
                .recommendedPlayers("9명 이상")
                .duration("2.5~3시간")
                .difficulty("전문가")
                .roles(new String[] { "탐정", "범인", "공범1", "공범2", "호텔 매니저",
                        "경비", "청소부", "투숙객A", "투숙객B" })
                .synopsis("세계적인 그랑 호텔에서 열린 갈라 디너. 정전과 함께 비명이 들렸고, " +
                        "불이 켜졌을 때 호텔 재벌이 쓰러져 있었다. 범인은 누구인가?")
                .build();
    }

    /**
     * 시나리오 추천 결과
     */
    @lombok.Builder
    @lombok.Getter
    public static class ScenarioRecommendation {
        private String title;
        private String description;
        private String recommendedPlayers;
        private String duration;
        private String difficulty;
        private String[] roles;
        private String synopsis;
    }
}
