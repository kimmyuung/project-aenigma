package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScenarioClue 엔티티 테스트
 */
@DisplayName("ScenarioClue 엔티티 테스트")
class ScenarioClueTest {

    private Scenario scenario;
    private ScenarioRole role;

    @BeforeEach
    void setUp() {
        User author = User.builder()
                .username("GUEST_author")
                .nickname("Author")
                .build();

        scenario = Scenario.builder()
                .title("미스터리 저택")
                .author(author)
                .minPlayers(4)
                .maxPlayers(6)
                .price(BigDecimal.ZERO)
                .build();

        role = ScenarioRole.builder()
                .scenario(scenario)
                .name("박비서")
                .roleType(GameRole.CRIMINAL)
                .build();
    }

    @Nested
    @DisplayName("isPersonal 테스트")
    class IsPersonalTest {

        @Test
        @DisplayName("PERSONAL 타입이고 역할이 있으면 true")
        void personalWithRoleReturnsTrue() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("비밀 편지")
                    .content("...숨겨진 내용...")
                    .clueType(ClueType.PERSONAL)
                    .assignedRole(role)
                    .build();

            // when & then
            assertThat(clue.isPersonal()).isTrue();
        }

        @Test
        @DisplayName("PERSONAL 타입이지만 역할이 없으면 false")
        void personalWithoutRoleReturnsFalse() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("비밀 단서")
                    .content("내용")
                    .clueType(ClueType.PERSONAL)
                    .assignedRole(null)
                    .build();

            // when & then
            assertThat(clue.isPersonal()).isFalse();
        }

        @Test
        @DisplayName("PUBLIC 타입은 false")
        void publicReturnsFalse() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("공개 단서")
                    .content("내용")
                    .clueType(ClueType.PUBLIC)
                    .build();

            // when & then
            assertThat(clue.isPersonal()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPublic 테스트")
    class IsPublicTest {

        @Test
        @DisplayName("PUBLIC 타입은 true")
        void publicReturnsTrue() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("공개 단서")
                    .content("모두가 볼 수 있는 단서")
                    .clueType(ClueType.PUBLIC)
                    .build();

            // when & then
            assertThat(clue.isPublic()).isTrue();
        }

        @Test
        @DisplayName("PERSONAL 타입은 false")
        void personalReturnsFalse() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("개인 단서")
                    .content("내용")
                    .clueType(ClueType.PERSONAL)
                    .build();

            // when & then
            assertThat(clue.isPublic()).isFalse();
        }

        @Test
        @DisplayName("HIDDEN 타입은 false")
        void hiddenReturnsFalse() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("숨겨진 단서")
                    .content("내용")
                    .clueType(ClueType.HIDDEN)
                    .build();

            // when & then
            assertThat(clue.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("isHidden 테스트")
    class IsHiddenTest {

        @Test
        @DisplayName("HIDDEN 타입은 true")
        void hiddenReturnsTrue() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("숨겨진 단서")
                    .content("발견해야 볼 수 있는 단서")
                    .clueType(ClueType.HIDDEN)
                    .build();

            // when & then
            assertThat(clue.isHidden()).isTrue();
        }

        @Test
        @DisplayName("PUBLIC 타입은 false")
        void publicReturnsFalse() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("공개 단서")
                    .content("내용")
                    .clueType(ClueType.PUBLIC)
                    .build();

            // when & then
            assertThat(clue.isHidden()).isFalse();
        }
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("clueType 기본값은 PUBLIC")
        void defaultClueTypeIsPublic() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("단서")
                    .content("내용")
                    .build();

            // then
            assertThat(clue.getClueType()).isEqualTo(ClueType.PUBLIC);
        }

        @Test
        @DisplayName("importance 기본값은 3")
        void defaultImportanceIsThree() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("단서")
                    .content("내용")
                    .build();

            // then
            assertThat(clue.getImportance()).isEqualTo(3);
        }

        @Test
        @DisplayName("displayOrder 기본값은 0")
        void defaultDisplayOrderIsZero() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("단서")
                    .content("내용")
                    .build();

            // then
            assertThat(clue.getDisplayOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("필드 설정 테스트")
    class FieldsTest {

        @Test
        @DisplayName("모든 필드가 올바르게 설정된다")
        void allFieldsAreSetCorrectly() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .scenario(scenario)
                    .title("피 묻은 칼")
                    .content("주방에서 발견된 흉기. 피해자의 혈액이 묻어있다.")
                    .clueType(ClueType.HIDDEN)
                    .assignedRole(role)
                    .revealPhase("INVESTIGATION")
                    .revealRound(2)
                    .imageUrl("https://example.com/knife.png")
                    .importance(5)
                    .displayOrder(1)
                    .build();

            // then
            assertThat(clue.getTitle()).isEqualTo("피 묻은 칼");
            assertThat(clue.getContent()).contains("주방");
            assertThat(clue.getClueType()).isEqualTo(ClueType.HIDDEN);
            assertThat(clue.getAssignedRole()).isEqualTo(role);
            assertThat(clue.getRevealPhase()).isEqualTo("INVESTIGATION");
            assertThat(clue.getRevealRound()).isEqualTo(2);
            assertThat(clue.getImageUrl()).contains("knife");
            assertThat(clue.getImportance()).isEqualTo(5);
            assertThat(clue.getDisplayOrder()).isEqualTo(1);
        }
    }
}
