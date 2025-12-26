package com.aenigma.domain.scenario.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 시나리오 구매 내역
 */
@Entity
@Table(name = "scenario_purchases", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "scenario_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScenarioPurchase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 구매자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User buyer;

    /**
     * 구매한 시나리오
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    /**
     * 구매 가격 (구매 시점 가격 기록)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    /**
     * 구매 일시
     */
    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    /**
     * 결제 ID (외부 결제 시스템)
     */
    private String paymentId;

    /**
     * 환불 여부
     */
    @Builder.Default
    private boolean refunded = false;

    /**
     * 환불 일시
     */
    private LocalDateTime refundedAt;

    // === 비즈니스 메서드 ===

    public void refund() {
        if (this.refunded) {
            throw new IllegalStateException("이미 환불된 구매입니다.");
        }
        this.refunded = true;
        this.refundedAt = LocalDateTime.now();
    }
}
