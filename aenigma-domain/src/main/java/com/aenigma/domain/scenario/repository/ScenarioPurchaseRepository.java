package com.aenigma.domain.scenario.repository;

import com.aenigma.domain.scenario.entity.ScenarioPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 시나리오 구매 Repository
 */
@Repository
public interface ScenarioPurchaseRepository extends JpaRepository<ScenarioPurchase, UUID> {

    /**
     * 특정 사용자의 특정 시나리오 구매 내역
     */
    Optional<ScenarioPurchase> findByBuyerIdAndScenarioId(UUID buyerId, UUID scenarioId);

    /**
     * 사용자의 구매 목록
     */
    List<ScenarioPurchase> findByBuyerIdAndRefundedFalse(UUID buyerId);

    /**
     * 시나리오 구매 여부 확인
     */
    boolean existsByBuyerIdAndScenarioIdAndRefundedFalse(UUID buyerId, UUID scenarioId);

    /**
     * 시나리오별 구매 수
     */
    long countByScenarioIdAndRefundedFalse(UUID scenarioId);
}
