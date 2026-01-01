package com.aenigma.domain.service;

import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.scenario.repository.*;
import com.aenigma.domain.scenario.service.ScenarioMarketService;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ScenarioMarketService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScenarioMarketService 테스트")
class ScenarioMarketServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private ScenarioPurchaseRepository purchaseRepository;
    @Mock
    private ScenarioReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScenarioMarketService marketService;

    private User author;
    private User buyer;
    private Scenario freeScenario;
    private Scenario paidScenario;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .nickname("Author")
                .username("GUEST_author")
                .build();
        setId(author, UUID.randomUUID());

        buyer = User.builder()
                .nickname("Buyer")
                .username("GUEST_buyer")
                .build();
        setId(buyer, UUID.randomUUID());

        freeScenario = Scenario.builder()
                .title("무료 시나리오")
                .author(author)
                .minPlayers(4)
                .maxPlayers(6)
                .price(BigDecimal.ZERO)
                .status(ScenarioStatus.PUBLISHED)
                .build();
        setId(freeScenario, UUID.randomUUID());

        paidScenario = Scenario.builder()
                .title("유료 시나리오")
                .author(author)
                .minPlayers(4)
                .maxPlayers(6)
                .price(new BigDecimal("9900"))
                .status(ScenarioStatus.PUBLISHED)
                .build();
        setId(paidScenario, UUID.randomUUID());
    }

    // 리플렉션으로 ID 설정 (테스트용)
    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getPublishedScenarios 테스트")
    class GetPublishedScenariosTest {

        @Test
        @DisplayName("공개된 시나리오 목록 조회 성공")
        void success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Scenario> page = new PageImpl<>(List.of(freeScenario, paidScenario));
            given(scenarioRepository.findByStatus(ScenarioStatus.PUBLISHED, pageable)).willReturn(page);

            // when
            Page<Scenario> result = marketService.getPublishedScenarios(pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("purchaseScenario 테스트")
    class PurchaseScenarioTest {

        @Test
        @DisplayName("시나리오 구매 성공")
        void success() {
            // given
            given(userRepository.findById(buyer.getId())).willReturn(Optional.of(buyer));
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));
            given(purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyer.getId(), paidScenario.getId()))
                    .willReturn(false);

            // when
            ScenarioPurchase purchase = marketService.purchaseScenario(
                    buyer.getId(), paidScenario.getId(), "PAY123");

            // then
            assertThat(purchase.getBuyer()).isEqualTo(buyer);
            assertThat(purchase.getScenario()).isEqualTo(paidScenario);
            assertThat(purchase.getPurchasePrice()).isEqualTo(new BigDecimal("9900"));
            verify(purchaseRepository).save(any(ScenarioPurchase.class));
        }

        @Test
        @DisplayName("이미 구매한 시나리오는 재구매 불가")
        void alreadyPurchased() {
            // given
            given(userRepository.findById(buyer.getId())).willReturn(Optional.of(buyer));
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));
            given(purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyer.getId(), paidScenario.getId()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> marketService.purchaseScenario(buyer.getId(), paidScenario.getId(), "PAY123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 구매한 시나리오");
        }

        @Test
        @DisplayName("본인 시나리오는 구매 불가")
        void cannotPurchaseOwnScenario() {
            // given
            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));

            // when & then
            assertThatThrownBy(() -> marketService.purchaseScenario(author.getId(), paidScenario.getId(), "PAY123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한 시나리오");
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 구매 불가")
        void userNotFound() {
            // given
            UUID unknownUserId = UUID.randomUUID();
            given(userRepository.findById(unknownUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> marketService.purchaseScenario(unknownUserId, paidScenario.getId(), "PAY123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("hasAccess 테스트")
    class HasAccessTest {

        @Test
        @DisplayName("무료 시나리오는 누구나 접근 가능")
        void freeScenarioAccessible() {
            // given
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));

            // when
            boolean result = marketService.hasAccess(buyer.getId(), freeScenario.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작가는 본인 시나리오에 접근 가능")
        void authorHasAccess() {
            // given
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));

            // when
            boolean result = marketService.hasAccess(author.getId(), paidScenario.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("구매자는 구매한 시나리오에 접근 가능")
        void buyerHasAccessAfterPurchase() {
            // given
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));
            given(purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyer.getId(), paidScenario.getId()))
                    .willReturn(true);

            // when
            boolean result = marketService.hasAccess(buyer.getId(), paidScenario.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("미구매자는 유료 시나리오에 접근 불가")
        void nonBuyerCannotAccess() {
            // given
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));
            given(purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyer.getId(), paidScenario.getId()))
                    .willReturn(false);

            // when
            boolean result = marketService.hasAccess(buyer.getId(), paidScenario.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("writeReview 테스트")
    class WriteReviewTest {

        @Test
        @DisplayName("무료 시나리오 리뷰 작성 성공")
        void reviewFreeScenario() {
            // given
            given(userRepository.findById(buyer.getId())).willReturn(Optional.of(buyer));
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));
            given(reviewRepository.existsByReviewerIdAndScenarioId(buyer.getId(), freeScenario.getId()))
                    .willReturn(false);

            // when
            ScenarioReview review = marketService.writeReview(
                    buyer.getId(), freeScenario.getId(), 5, "최고의 시나리오!", false);

            // then
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getContent()).isEqualTo("최고의 시나리오!");
            verify(reviewRepository).save(any(ScenarioReview.class));
        }

        @Test
        @DisplayName("구매하지 않은 유료 시나리오는 리뷰 불가")
        void cannotReviewUnpurchasedPaidScenario() {
            // given
            given(userRepository.findById(buyer.getId())).willReturn(Optional.of(buyer));
            given(scenarioRepository.findById(paidScenario.getId())).willReturn(Optional.of(paidScenario));
            given(purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyer.getId(), paidScenario.getId()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> marketService.writeReview(
                    buyer.getId(), paidScenario.getId(), 4, "좋아요", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("구매한 시나리오만 리뷰할 수 있습니다");
        }

        @Test
        @DisplayName("이미 리뷰를 작성한 경우 재작성 불가")
        void cannotWriteMultipleReviews() {
            // given
            given(userRepository.findById(buyer.getId())).willReturn(Optional.of(buyer));
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));
            given(reviewRepository.existsByReviewerIdAndScenarioId(buyer.getId(), freeScenario.getId()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> marketService.writeReview(
                    buyer.getId(), freeScenario.getId(), 4, "또 리뷰", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 리뷰를 작성했습니다");
        }

        @Test
        @DisplayName("본인 시나리오에는 리뷰 불가")
        void cannotReviewOwnScenario() {
            // given
            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));

            // when & then
            assertThatThrownBy(() -> marketService.writeReview(
                    author.getId(), freeScenario.getId(), 5, "셀프 리뷰", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인 시나리오는 리뷰할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("createScenario 테스트")
    class CreateScenarioTest {

        @Test
        @DisplayName("시나리오 생성 성공")
        void success() {
            // given
            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));

            // when
            Scenario result = marketService.createScenario(
                    author.getId(), "새 시나리오", "설명", 4, 6, new BigDecimal("5000"));

            // then
            assertThat(result.getTitle()).isEqualTo("새 시나리오");
            assertThat(result.getAuthor()).isEqualTo(author);
            assertThat(result.getStatus()).isEqualTo(ScenarioStatus.DRAFT);
            verify(scenarioRepository).save(any(Scenario.class));
        }
    }

    @Nested
    @DisplayName("publishScenario 테스트")
    class PublishScenarioTest {

        @Test
        @DisplayName("본인 시나리오만 공개 가능")
        void onlyAuthorCanPublish() {
            // given - 역할 추가된 시나리오
            ScenarioRole role = ScenarioRole.builder().name("형사").build();
            freeScenario.addRole(role);
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));

            // when
            marketService.publishScenario(author.getId(), freeScenario.getId());

            // then
            assertThat(freeScenario.getStatus()).isEqualTo(ScenarioStatus.PUBLISHED);
        }

        @Test
        @DisplayName("다른 사람의 시나리오는 공개 불가")
        void cannotPublishOthersScenario() {
            // given
            given(scenarioRepository.findById(freeScenario.getId())).willReturn(Optional.of(freeScenario));

            // when & then
            assertThatThrownBy(() -> marketService.publishScenario(buyer.getId(), freeScenario.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인의 시나리오만 공개할 수 있습니다");
        }
    }
}
