package com.aenigma.ai.collector.service;

import com.aenigma.ai.collector.entity.RoundSummary;
import com.aenigma.ai.collector.repository.RoundSummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 라운드 요약 서비스
 * 
 * GM이 입력한 라운드별 대화 요약을 관리합니다.
 * 음성 대화 내용을 AI 학습 데이터로 활용할 수 있게 합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoundSummaryService {

    private final RoundSummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 라운드 요약 기록
     */
    public RoundSummary recordSummary(UUID gameId, int round, String phase,
            String summary, String recordedBy) {
        // 기존 요약이 있으면 업데이트
        RoundSummary existing = summaryRepository.findByGameIdAndRound(gameId, round)
                .orElse(null);

        if (existing != null) {
            log.info("라운드 요약 업데이트: gameId={}, round={}", gameId, round);
            // 기존 엔티티 업데이트 로직 필요시 추가
            return existing;
        }

        RoundSummary roundSummary = RoundSummary.create(gameId, round, phase, summary, recordedBy);
        RoundSummary saved = summaryRepository.save(roundSummary);

        log.info("라운드 요약 기록: gameId={}, round={}, phase={}", gameId, round, phase);
        return saved;
    }

    /**
     * 주요 이벤트 추가 (JSON 배열)
     */
    public void addKeyEvents(UUID gameId, int round, List<String> events) {
        summaryRepository.findByGameIdAndRound(gameId, round).ifPresent(summary -> {
            try {
                String json = objectMapper.writeValueAsString(events);
                summary.updateKeyEvents(json);
                log.debug("주요 이벤트 추가: gameId={}, round={}, count={}", gameId, round, events.size());
            } catch (JsonProcessingException e) {
                log.error("주요 이벤트 JSON 변환 실패", e);
            }
        });
    }

    /**
     * 의심 관계 기록 (누가 누구를 의심했는지)
     */
    public void recordSuspicions(UUID gameId, int round, Map<String, List<String>> suspicions) {
        summaryRepository.findByGameIdAndRound(gameId, round).ifPresent(summary -> {
            try {
                String json = objectMapper.writeValueAsString(suspicions);
                summary.updateSuspicions(json);
                log.debug("의심 관계 기록: gameId={}, round={}", gameId, round);
            } catch (JsonProcessingException e) {
                log.error("의심 관계 JSON 변환 실패", e);
            }
        });
    }

    // === 조회 메서드 ===

    @Transactional(readOnly = true)
    public List<RoundSummary> getGameSummaries(UUID gameId) {
        return summaryRepository.findByGameIdOrderByRoundAsc(gameId);
    }

    @Transactional(readOnly = true)
    public RoundSummary getRoundSummary(UUID gameId, int round) {
        return summaryRepository.findByGameIdAndRound(gameId, round).orElse(null);
    }

    @Transactional(readOnly = true)
    public long getSummaryCount(UUID gameId) {
        return summaryRepository.countByGameId(gameId);
    }

    /**
     * 학습 데이터용 요약 텍스트 생성
     */
    @Transactional(readOnly = true)
    public String buildSummaryText(UUID gameId) {
        List<RoundSummary> summaries = getGameSummaries(gameId);
        if (summaries.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (RoundSummary s : summaries) {
            sb.append(String.format("[라운드 %d - %s]\n%s\n\n",
                    s.getRound(), s.getPhase(), s.getSummary()));
        }
        return sb.toString();
    }
}
