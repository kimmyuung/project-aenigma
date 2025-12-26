package com.aenigma.domain.scenario.repository;

import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.entity.ScenarioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 시나리오 Repository
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {

    /**
     * 공개된 시나리오 목록 (페이징)
     */
    Page<Scenario> findByStatus(ScenarioStatus status, Pageable pageable);

    /**
     * 작가별 시나리오 목록
     */
    List<Scenario> findByAuthorId(UUID authorId);

    /**
     * 플레이어 수로 필터링된 공개 시나리오
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' " +
            "AND s.minPlayers <= :playerCount AND s.maxPlayers >= :playerCount")
    Page<Scenario> findPublishedByPlayerCount(@Param("playerCount") int playerCount, Pageable pageable);

    /**
     * 태그로 검색
     */
    @Query("SELECT s FROM Scenario s JOIN s.tags t WHERE s.status = 'PUBLISHED' AND t = :tag")
    Page<Scenario> findPublishedByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 제목 또는 설명으로 검색
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Scenario> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 인기 시나리오 (다운로드 순)
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' ORDER BY s.downloadCount DESC")
    Page<Scenario> findPopular(Pageable pageable);

    /**
     * 평점 높은 시나리오
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' AND s.reviewCount >= :minReviews " +
            "ORDER BY s.averageRating DESC")
    Page<Scenario> findTopRated(@Param("minReviews") int minReviews, Pageable pageable);

    /**
     * 무료 시나리오
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' AND s.price = 0")
    Page<Scenario> findFreeScenarios(Pageable pageable);

    /**
     * 최신 시나리오
     */
    @Query("SELECT s FROM Scenario s WHERE s.status = 'PUBLISHED' ORDER BY s.createdAt DESC")
    Page<Scenario> findLatest(Pageable pageable);
}
