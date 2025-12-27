package com.aenigma.domain.scenario.parser;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.ClueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MarkdownScenarioParser 단위 테스트
 */
class MarkdownScenarioParserTest {

    private MarkdownScenarioParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownScenarioParser();
    }

    @Nested
    @DisplayName("기본 정보 파싱")
    class BasicInfoParsing {

        @Test
        @DisplayName("시나리오 제목을 파싱한다")
        void parseTitle() {
            // given
            String content = """
                    # 🎭 시나리오: 붉은 저택의 비밀

                    ## 📖 기본 정보
                    - 플레이어: [4~6]명

                    ## 🎭 역할
                    ### [CRIMINAL] 레이디
                    - **설명**: 테스트
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getTitle()).isEqualTo("붉은 저택의 비밀");
        }

        @Test
        @DisplayName("플레이어 수를 파싱한다")
        void parsePlayerCount() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 기본 정보
                    - 플레이어: [4~8]명
                    - 예상 시간: [150]분
                    - 난이도: [4]

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getMinPlayers()).isEqualTo(4);
            assertThat(result.getMaxPlayers()).isEqualTo(8);
            assertThat(result.getEstimatedMinutes()).isEqualTo(150);
            assertThat(result.getDifficulty()).isEqualTo(4);
        }

        @Test
        @DisplayName("태그를 파싱한다")
        void parseTags() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 기본 정보
                    - 플레이어: [4~6]명
                    - 태그: 미스터리, 스릴러, 호러

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getTags()).containsExactly("미스터리", "스릴러", "호러");
        }

        @Test
        @DisplayName("배경 스토리를 파싱한다")
        void parseBackgroundStory() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 배경 스토리
                    1920년대 영국의 어느 저택.
                    비가 내리는 밤, 사건이 발생한다.

                    ---

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getBackgroundStory()).contains("1920년대 영국");
            assertThat(result.getBackgroundStory()).contains("비가 내리는 밤");
        }
    }

    @Nested
    @DisplayName("역할 파싱")
    class RoleParsing {

        @Test
        @DisplayName("CRIMINAL 역할을 파싱한다")
        void parseCriminalRole() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할

                    ### [CRIMINAL] 레이디 엘리자베스
                    - **설명**: 피해자의 아내. 35세.
                    - **비밀**: 남편을 독살했다.
                    - **목표**: 의심받지 않고 살아남아야 한다.
                    - **관계**: 정원사와 친밀한 사이.
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getRoles()).hasSize(1);

            ParsedScenario.ParsedRole role = result.getRoles().get(0);
            assertThat(role.getName()).isEqualTo("레이디 엘리자베스");
            assertThat(role.getRoleType()).isEqualTo(GameRole.CRIMINAL);
            assertThat(role.getDescription()).contains("피해자의 아내");
            assertThat(role.getSecretInfo()).contains("독살");
            assertThat(role.getObjective()).contains("살아남아야");
            assertThat(role.getRelationships()).contains("정원사");
        }

        @Test
        @DisplayName("DETECTIVE 역할을 파싱한다")
        void parseDetectiveRole() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할

                    ### [DETECTIVE] 셜록 홈즈
                    - **설명**: 유명한 탐정. 42세.
                    - **비밀**: 과거의 트라우마가 있다.
                    - **목표**: 범인을 찾아내야 한다.
                    - **관계**: 왓슨과 친구 사이.
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            ParsedScenario.ParsedRole role = result.getRoles().get(0);
            assertThat(role.getRoleType()).isEqualTo(GameRole.DETECTIVE);
        }

        @Test
        @DisplayName("SUSPECT 역할을 파싱한다")
        void parseSuspectRole() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할

                    ### [SUSPECT] 정원사 톰
                    - **설명**: 저택의 정원사. 25세.
                    - **비밀**: 레이디를 좋아한다.
                    - **목표**: 레이디를 보호해야 한다.
                    - **관계**: 레이디와 친밀함.
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            ParsedScenario.ParsedRole role = result.getRoles().get(0);
            assertThat(role.getRoleType()).isEqualTo(GameRole.SUSPECT);
        }

        @Test
        @DisplayName("여러 역할을 순서대로 파싱한다")
        void parseMultipleRoles() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할

                    ### [CRIMINAL] 범인A
                    - **설명**: 범인 설명

                    ### [DETECTIVE] 탐정B
                    - **설명**: 탐정 설명

                    ### [SUSPECT] 용의자C
                    - **설명**: 용의자 설명
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getRoles()).hasSize(3);
            assertThat(result.getRoles().get(0).getName()).isEqualTo("범인A");
            assertThat(result.getRoles().get(0).getDisplayOrder()).isEqualTo(0);
            assertThat(result.getRoles().get(1).getName()).isEqualTo("탐정B");
            assertThat(result.getRoles().get(1).getDisplayOrder()).isEqualTo(1);
            assertThat(result.getRoles().get(2).getName()).isEqualTo("용의자C");
            assertThat(result.getRoles().get(2).getDisplayOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("단서 파싱")
    class ClueParsing {

        @Test
        @DisplayName("PUBLIC 단서를 파싱한다")
        void parsePublicClue() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트

                    ## 단서

                    ### [PUBLIC] 독이 든 와인잔
                    - **내용**: 피해자 옆에서 발견된 와인잔. 비소가 검출됨.
                    - **라운드**: 1
                    - **중요도**: 5
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getClues()).hasSize(1);

            ParsedScenario.ParsedClue clue = result.getClues().get(0);
            assertThat(clue.getTitle()).isEqualTo("독이 든 와인잔");
            assertThat(clue.getClueType()).isEqualTo(ClueType.PUBLIC);
            assertThat(clue.getContent()).contains("비소");
            assertThat(clue.getRevealRound()).isEqualTo(1);
            assertThat(clue.getImportance()).isEqualTo(5);
        }

        @Test
        @DisplayName("PERSONAL 단서를 파싱한다 (역할 지정)")
        void parsePersonalClue() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트

                    ## 단서

                    ### [PERSONAL:셜록 홈즈] 오래된 신문 기사
                    - **내용**: 10년 전 사건에 대한 기사.
                    - **라운드**: 2
                    - **중요도**: 3
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            ParsedScenario.ParsedClue clue = result.getClues().get(0);
            assertThat(clue.getClueType()).isEqualTo(ClueType.PERSONAL);
            assertThat(clue.getAssignedRoleName()).isEqualTo("셜록 홈즈");
        }

        @Test
        @DisplayName("HIDDEN 단서를 파싱한다")
        void parseHiddenClue() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트

                    ## 단서

                    ### [HIDDEN] 창고의 비소
                    - **내용**: 숨겨진 비소 병.
                    - **라운드**: 3
                    - **중요도**: 5
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            ParsedScenario.ParsedClue clue = result.getClues().get(0);
            assertThat(clue.getClueType()).isEqualTo(ClueType.HIDDEN);
        }

        @Test
        @DisplayName("여러 단서를 순서대로 파싱한다")
        void parseMultipleClues() {
            // given
            String content = """
                    # 시나리오: 테스트

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트

                    ## 단서

                    ### [PUBLIC] 단서1
                    - **내용**: 내용1
                    - **라운드**: 1

                    ### [PERSONAL:역할A] 단서2
                    - **내용**: 내용2
                    - **라운드**: 2

                    ### [HIDDEN] 단서3
                    - **내용**: 내용3
                    - **라운드**: 3
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            assertThat(result.getClues()).hasSize(3);
            assertThat(result.getClues().get(0).getDisplayOrder()).isEqualTo(0);
            assertThat(result.getClues().get(1).getDisplayOrder()).isEqualTo(1);
            assertThat(result.getClues().get(2).getDisplayOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("빈 내용이면 예외를 던진다")
        void throwExceptionForEmptyContent() {
            // given
            String content = "";

            // when & then
            assertThatThrownBy(() -> parser.parse(content))
                    .isInstanceOf(ScenarioParseException.class)
                    .hasMessageContaining("비어있습니다");
        }

        @Test
        @DisplayName("null 내용이면 예외를 던진다")
        void throwExceptionForNullContent() {
            // when & then
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(ScenarioParseException.class);
        }

        @Test
        @DisplayName("제목이 없으면 예외를 던진다")
        void throwExceptionForMissingTitle() {
            // given
            String content = """
                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;

            // when & then
            assertThatThrownBy(() -> parser.parse(content))
                    .isInstanceOf(ScenarioParseException.class)
                    .hasMessageContaining("제목");
        }
    }

    @Nested
    @DisplayName("전체 시나리오 파싱")
    class FullScenarioParsing {

        @Test
        @DisplayName("완전한 시나리오를 파싱한다")
        void parseFullScenario() {
            // given
            String content = """
                    # 🎭 시나리오: 붉은 저택의 비밀

                    ## 📖 기본 정보
                    - 플레이어: [4~6]명
                    - 예상 시간: [120]분
                    - 난이도: [3]
                    - 태그: 미스터리, 스릴러

                    ## 📜 배경 스토리
                    1920년대 영국, 윈체스터 저택에서 당주 에드워드 경이
                    서재에서 독살된 채 발견된다.

                    ---

                    ## 🎭 역할

                    ### [CRIMINAL] 레이디 엘리자베스
                    - **설명**: 피해자의 아내. 35세.
                    - **비밀**: 남편을 독살했다.
                    - **목표**: 의심받지 않아야 한다.
                    - **관계**: 정원사와 친밀함.

                    ### [DETECTIVE] 셜록 홈즈
                    - **설명**: 유명한 탐정. 42세.
                    - **비밀**: 과거 원한이 있다.
                    - **목표**: 범인을 찾아야 한다.
                    - **관계**: 왓슨과 친구.

                    ---

                    ## 🔍 단서

                    ### [PUBLIC] 독이 든 와인잔
                    - **내용**: 비소가 검출된 와인잔.
                    - **라운드**: 1
                    - **중요도**: 5

                    ### [PERSONAL:셜록 홈즈] 오래된 신문
                    - **내용**: 10년 전 사건 기사.
                    - **라운드**: 1
                    - **중요도**: 3

                    ### [HIDDEN] 창고의 비소
                    - **내용**: 숨겨진 독약 병.
                    - **라운드**: 3
                    - **중요도**: 5
                    """;

            // when
            ParsedScenario result = parser.parse(content);

            // then
            // 기본 정보
            assertThat(result.getTitle()).isEqualTo("붉은 저택의 비밀");
            assertThat(result.getMinPlayers()).isEqualTo(4);
            assertThat(result.getMaxPlayers()).isEqualTo(6);
            assertThat(result.getEstimatedMinutes()).isEqualTo(120);
            assertThat(result.getDifficulty()).isEqualTo(3);
            assertThat(result.getTags()).containsExactly("미스터리", "스릴러");
            assertThat(result.getBackgroundStory()).contains("1920년대 영국");

            // 역할
            assertThat(result.getRoles()).hasSize(2);
            assertThat(result.getRoles().get(0).getRoleType()).isEqualTo(GameRole.CRIMINAL);
            assertThat(result.getRoles().get(1).getRoleType()).isEqualTo(GameRole.DETECTIVE);

            // 단서
            assertThat(result.getClues()).hasSize(3);
            assertThat(result.getClues().get(0).getClueType()).isEqualTo(ClueType.PUBLIC);
            assertThat(result.getClues().get(1).getClueType()).isEqualTo(ClueType.PERSONAL);
            assertThat(result.getClues().get(1).getAssignedRoleName()).isEqualTo("셜록 홈즈");
            assertThat(result.getClues().get(2).getClueType()).isEqualTo(ClueType.HIDDEN);
        }
    }
}
