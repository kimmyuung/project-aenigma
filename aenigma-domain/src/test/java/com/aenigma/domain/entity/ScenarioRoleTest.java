package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.entity.ScenarioRole;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScenarioRole 엔티티 테스트
 */
@DisplayName("ScenarioRole 엔티티 테스트")
class ScenarioRoleTest {

    private Scenario scenario;

    @BeforeEach
    void setUp() {
        User author = User.builder()
                .username("GUEST_author")
                .nickname("Author")
                .build();

        scenario = Scenario.builder()
                .title("살인 미스터리")
                .author(author)
                .minPlayers(4)
                .maxPlayers(6)
                .price(BigDecimal.ZERO)
                .build();
    }

    @Nested
    @DisplayName("isCriminal 테스트")
    class IsCriminalTest {

        @Test
        @DisplayName("CRIMINAL 역할은 true 반환")
        void criminalReturnsTrue() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("박비서")
                    .roleType(GameRole.CRIMINAL)
                    .description("사건의 진범")
                    .build();

            // when & then
            assertThat(role.isCriminal()).isTrue();
        }

        @Test
        @DisplayName("SUSPECT 역할은 false 반환")
        void suspectReturnsFalse() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("김대리")
                    .roleType(GameRole.SUSPECT)
                    .description("용의자")
                    .build();

            // when & then
            assertThat(role.isCriminal()).isFalse();
        }

        @Test
        @DisplayName("DETECTIVE 역할은 false 반환")
        void detectiveReturnsFalse() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("이형사")
                    .roleType(GameRole.DETECTIVE)
                    .description("사건 담당 형사")
                    .build();

            // when & then
            assertThat(role.isCriminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDetective 테스트")
    class IsDetectiveTest {

        @Test
        @DisplayName("DETECTIVE 역할은 true 반환")
        void detectiveReturnsTrue() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("이형사")
                    .roleType(GameRole.DETECTIVE)
                    .build();

            // when & then
            assertThat(role.isDetective()).isTrue();
        }

        @Test
        @DisplayName("CRIMINAL 역할은 false 반환")
        void criminalReturnsFalse() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("박비서")
                    .roleType(GameRole.CRIMINAL)
                    .build();

            // when & then
            assertThat(role.isDetective()).isFalse();
        }

        @Test
        @DisplayName("SUSPECT 역할은 false 반환")
        void suspectReturnsFalse() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("최박사")
                    .roleType(GameRole.SUSPECT)
                    .build();

            // when & then
            assertThat(role.isDetective()).isFalse();
        }
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("displayOrder 기본값은 0")
        void defaultDisplayOrderIsZero() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("테스트 역할")
                    .roleType(GameRole.SUSPECT)
                    .build();

            // then
            assertThat(role.getDisplayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("personalClues 기본값은 빈 리스트")
        void defaultPersonalCluesIsEmptyList() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("테스트 역할")
                    .roleType(GameRole.SUSPECT)
                    .build();

            // then
            assertThat(role.getPersonalClues()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("필드 설정 테스트")
    class FieldsTest {

        @Test
        @DisplayName("모든 필드가 올바르게 설정된다")
        void allFieldsAreSetCorrectly() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .scenario(scenario)
                    .name("정 변호사")
                    .roleType(GameRole.SUSPECT)
                    .description("피해자의 고문 변호사")
                    .secretInfo("피해자에게 빚이 있다")
                    .objective("빚 사실을 숨기고 살아남기")
                    .relationships("피해자와의 관계: 채권-채무 관계")
                    .imageUrl("https://example.com/lawyer.png")
                    .displayOrder(2)
                    .build();

            // then
            assertThat(role.getName()).isEqualTo("정 변호사");
            assertThat(role.getRoleType()).isEqualTo(GameRole.SUSPECT);
            assertThat(role.getDescription()).isEqualTo("피해자의 고문 변호사");
            assertThat(role.getSecretInfo()).isEqualTo("피해자에게 빚이 있다");
            assertThat(role.getObjective()).isEqualTo("빚 사실을 숨기고 살아남기");
            assertThat(role.getRelationships()).isEqualTo("피해자와의 관계: 채권-채무 관계");
            assertThat(role.getImageUrl()).isEqualTo("https://example.com/lawyer.png");
            assertThat(role.getDisplayOrder()).isEqualTo(2);
        }
    }
}
