package com.aenigma.domain.scenario.service;

import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.scenario.repository.*;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 시나리오 마켓플레이스 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScenarioMarketService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioPurchaseRepository purchaseRepository;
    private final ScenarioReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // === 시나리오 조회 ===

    @Transactional(readOnly = true)
    public Page<Scenario> getPublishedScenarios(Pageable pageable) {
        return scenarioRepository.findByStatus(ScenarioStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> searchScenarios(String keyword, Pageable pageable) {
        return scenarioRepository.searchPublished(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> getPopularScenarios(Pageable pageable) {
        return scenarioRepository.findPopular(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> getTopRatedScenarios(int minReviews, Pageable pageable) {
        return scenarioRepository.findTopRated(minReviews, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> getFreeScenarios(Pageable pageable) {
        return scenarioRepository.findFreeScenarios(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> getLatestScenarios(Pageable pageable) {
        return scenarioRepository.findLatest(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Scenario> filterByPlayerCount(int playerCount, Pageable pageable) {
        return scenarioRepository.findPublishedByPlayerCount(playerCount, pageable);
    }

    @Transactional(readOnly = true)
    public Scenario getScenarioDetail(UUID scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));
        scenario.incrementViewCount();
        return scenario;
    }

    // === 구매 ===

    /**
     * 시나리오 구매 (또는 무료 다운로드)
     */
    public ScenarioPurchase purchaseScenario(UUID buyerId, UUID scenarioId, String paymentId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + buyerId));

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));

        // 이미 구매했는지 확인
        if (purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(buyerId, scenarioId)) {
            throw new IllegalStateException("이미 구매한 시나리오입니다.");
        }

        // 본인 시나리오인지 확인
        if (scenario.getAuthor().getId().equals(buyerId)) {
            throw new IllegalStateException("본인이 작성한 시나리오입니다.");
        }

        ScenarioPurchase purchase = ScenarioPurchase.builder()
                .buyer(buyer)
                .scenario(scenario)
                .purchasePrice(scenario.getPrice())
                .purchasedAt(LocalDateTime.now())
                .paymentId(paymentId)
                .build();

        scenario.incrementDownloadCount();
        purchaseRepository.save(purchase);

        log.info("시나리오 구매 완료: buyerId={}, scenarioId={}, price={}",
                buyerId, scenarioId, scenario.getPrice());

        return purchase;
    }

    /**
     * 구매 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasPurchased(UUID userId, UUID scenarioId) {
        return purchaseRepository.existsByBuyerIdAndScenarioIdAndRefundedFalse(userId, scenarioId);
    }

    /**
     * 시나리오 접근 권한 확인 (작가 또는 구매자)
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null)
            return false;

        // 무료 시나리오
        if (scenario.isFree())
            return true;

        // 작가
        if (scenario.getAuthor().getId().equals(userId))
            return true;

        // 구매자
        return hasPurchased(userId, scenarioId);
    }

    // === 리뷰 ===

    /**
     * 리뷰 작성
     */
    public ScenarioReview writeReview(UUID reviewerId, UUID scenarioId,
            int rating, String content, boolean hasSpoiler) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + reviewerId));

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));

        // 구매했거나 무료 시나리오인 경우만 리뷰 가능
        if (!scenario.isFree() && !hasPurchased(reviewerId, scenarioId)) {
            throw new IllegalStateException("구매한 시나리오만 리뷰할 수 있습니다.");
        }

        // 이미 리뷰했는지 확인
        if (reviewRepository.existsByReviewerIdAndScenarioId(reviewerId, scenarioId)) {
            throw new IllegalStateException("이미 리뷰를 작성했습니다.");
        }

        // 본인 시나리오 리뷰 불가
        if (scenario.getAuthor().getId().equals(reviewerId)) {
            throw new IllegalStateException("본인 시나리오는 리뷰할 수 없습니다.");
        }

        ScenarioReview review = ScenarioReview.builder()
                .reviewer(reviewer)
                .scenario(scenario)
                .rating(rating)
                .content(content)
                .hasSpoiler(hasSpoiler)
                .build();

        reviewRepository.save(review);
        scenario.updateRating(rating);

        log.info("리뷰 작성 완료: reviewerId={}, scenarioId={}, rating={}",
                reviewerId, scenarioId, rating);

        return review;
    }

    @Transactional(readOnly = true)
    public Page<ScenarioReview> getReviews(UUID scenarioId, boolean includeSpoilers, Pageable pageable) {
        if (includeSpoilers) {
            return reviewRepository.findByScenarioId(scenarioId, pageable);
        }
        return reviewRepository.findByScenarioIdAndHasSpoilerFalse(scenarioId, pageable);
    }

    // === 작가 기능 ===

    /**
     * 시나리오 생성
     */
    public Scenario createScenario(UUID authorId, String title, String description,
            int minPlayers, int maxPlayers, BigDecimal price) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + authorId));

        Scenario scenario = Scenario.builder()
                .author(author)
                .title(title)
                .description(description)
                .minPlayers(minPlayers)
                .maxPlayers(maxPlayers)
                .price(price)
                .status(ScenarioStatus.DRAFT)
                .build();

        scenarioRepository.save(scenario);
        log.info("시나리오 생성: authorId={}, title={}", authorId, title);

        return scenario;
    }

    /**
     * 시나리오 공개
     */
    public void publishScenario(UUID authorId, UUID scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));

        if (!scenario.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("본인의 시나리오만 공개할 수 있습니다.");
        }

        scenario.publish();
        log.info("시나리오 공개: scenarioId={}", scenarioId);
    }
}
