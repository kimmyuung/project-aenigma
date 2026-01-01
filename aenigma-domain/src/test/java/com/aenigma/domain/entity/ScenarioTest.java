package com.aenigma.domain.entity;

import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scenario 엔티티 테스트
 */
@DisplayName("Scenario 엔티티 테스트")
class ScenarioTest {

    private User author;
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .nickname("AuthorUser")
                .username("GUEST_author")
                .build();

        scenario = Scenario.builder()
                .title("미스터리 맨션")
                .description("으리으리한 저택에서 벌어진 살인 사건")
                .author(author)
                .minPlayers(4)
                .maxPlayers(6)
                .estimatedMinutes(120)
                .difficulty(3)
                .price(BigDecimal.ZERO)
                .status(ScenarioStatus.DRAFT)
                .build();
    }

    @Nested
    @DisplayName("publish 테스트")
    class PublishTest {

        @Test
        @DisplayName("역할이 있으면 공개 가능")
        void canPublishWithRoles() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .name("형사")
                    .description("사건을 조사하는 형사")
                    .build();
            scenario.addRole(role);

            // when
            scenario.publish();

            // then
            assertThat(scenario.getStatus()).isEqualTo(ScenarioStatus.PUBLISHED);
        }

        @Test
        @DisplayName("역할이 없으면 공개 불가")
        void cannotPublishWithoutRoles() {
            // given - 역할 없음

            // when & then
            assertThatThrownBy(() -> scenario.publish())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("역할이 없는 시나리오는 공개할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("unpublish 테스트")
    class UnpublishTest {

        @Test
        @DisplayName("공개된 시나리오를 비공개로 전환")
        void canUnpublishPublishedScenario() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .name("형사")
                    .build();
            scenario.addRole(role);
            scenario.publish();

            // when
            scenario.unpublish();

            // then
            assertThat(scenario.getStatus()).isEqualTo(ScenarioStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("조회수/다운로드 카운터 테스트")
    class CounterTest {

        @Test
        @DisplayName("조회수 증가")
        void incrementViewCount() {
            // given
            long initialCount = scenario.getViewCount();

            // when
            scenario.incrementViewCount();
            scenario.incrementViewCount();

            // then
            assertThat(scenario.getViewCount()).isEqualTo(initialCount + 2);
        }

        @Test
        @DisplayName("다운로드수 증가")
        void incrementDownloadCount() {
            // given
            long initialCount = scenario.getDownloadCount();

            // when
            scenario.incrementDownloadCount();

            // then
            assertThat(scenario.getDownloadCount()).isEqualTo(initialCount + 1);
        }
    }

    @Nested
    @DisplayName("평점 테스트")
    class RatingTest {

        @Test
        @DisplayName("첫 평점 업데이트")
        void firstRating() {
            // when
            scenario.updateRating(4.0);

            // then
            assertThat(scenario.getAverageRating()).isEqualTo(4.0);
            assertThat(scenario.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("여러 평점의 평균 계산")
        void multipleRatings() {
            // when
            scenario.updateRating(5.0);
            scenario.updateRating(3.0);
            scenario.updateRating(4.0);

            // then
            assertThat(scenario.getReviewCount()).isEqualTo(3);
            assertThat(scenario.getAverageRating()).isEqualTo(4.0);
        }
    }

    @Nested
    @DisplayName("isFree 테스트")
    class IsFreeTest {

        @Test
        @DisplayName("가격이 0이면 무료")
        void zeroPrice() {
            assertThat(scenario.isFree()).isTrue();
        }

        @Test
        @DisplayName("가격이 있으면 유료")
        void nonZeroPrice() {
            // given
            Scenario paidScenario = Scenario.builder()
                    .title("유료 시나리오")
                    .author(author)
                    .minPlayers(4)
                    .maxPlayers(6)
                    .price(new BigDecimal("9900"))
                    .build();

            // then
            assertThat(paidScenario.isFree()).isFalse();
        }
    }

    @Nested
    @DisplayName("역할/단서 추가 테스트")
    class AddRoleAndClueTest {

        @Test
        @DisplayName("역할 추가 시 양방향 연관관계 설정")
        void addRoleSetsRelationship() {
            // given
            ScenarioRole role = ScenarioRole.builder()
                    .name("범인")
                    .description("살인을 저지른 인물")
                    .build();

            // when
            scenario.addRole(role);

            // then
            assertThat(scenario.getRoles()).contains(role);
            assertThat(role.getScenario()).isEqualTo(scenario);
        }

        @Test
        @DisplayName("단서 추가 시 양방향 연관관계 설정")
        void addClueSetsRelationship() {
            // given
            ScenarioClue clue = ScenarioClue.builder()
                    .title("피 묻은 칼")
                    .content("주방에서 발견된 흉기")
                    .clueType(ClueType.PUBLIC)
                    .build();

            // when
            scenario.addClue(clue);

            // then
            assertThat(scenario.getClues()).contains(clue);
            assertThat(clue.getScenario()).isEqualTo(scenario);
        }
    }

    @Nested
    @DisplayName("caseSummary 테스트")
    class CaseSummaryTest {

        @Test
        @DisplayName("caseSummary 설정 및 조회")
        void caseSummaryCanBeSetAndRetrieved() {
            // given
            Scenario scenarioWithSummary = Scenario.builder()
                    .title("살인 사건")
                    .author(author)
                    .minPlayers(4)
                    .maxPlayers(6)
                    .price(BigDecimal.ZERO)
                    .caseSummary("범인은 박비서였다. 그녀는 복수를 위해 독을 탔다.")
                    .build();

            // then
            assertThat(scenarioWithSummary.getCaseSummary())
                    .isEqualTo("범인은 박비서였다. 그녀는 복수를 위해 독을 탔다.");
        }
    }
}
