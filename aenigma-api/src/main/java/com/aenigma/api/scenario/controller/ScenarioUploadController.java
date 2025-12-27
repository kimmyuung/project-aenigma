package com.aenigma.api.scenario.controller;

import com.aenigma.api.scenario.dto.ScenarioUploadRequest;
import com.aenigma.api.scenario.dto.ScenarioUploadResponse;
import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.parser.ScenarioParseException;
import com.aenigma.domain.scenario.service.ScenarioUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 시나리오 업로드 컨트롤러
 */
@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
@Slf4j
public class ScenarioUploadController {

    private final ScenarioUploadService uploadService;

    /**
     * Markdown 파일로 시나리오 업로드
     * 
     * @param file        Markdown 파일 (.md)
     * @param price       가격 (선택, 기본 무료)
     * @param userDetails 인증된 사용자
     * @return 업로드 결과
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScenarioUploadResponse> uploadScenario(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 파일 검증
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("Markdown 파일(.md)만 업로드 가능합니다.");
        }

        try {
            // 파일 내용 읽기
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 사용자 ID 추출 (UserDetails에서)
            UUID authorId = UUID.fromString(userDetails.getUsername());

            // 업로드 처리
            Scenario scenario = uploadService.uploadFromMarkdown(authorId, content, price);

            log.info("시나리오 업로드 성공: userId={}, scenarioId={}", authorId, scenario.getId());

            return ResponseEntity.ok(ScenarioUploadResponse.from(scenario));

        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            throw new IllegalArgumentException("파일을 읽을 수 없습니다: " + e.getMessage());
        } catch (ScenarioParseException e) {
            log.warn("시나리오 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 텍스트 내용으로 직접 시나리오 업로드
     * 
     * @param content     Markdown 텍스트 내용
     * @param price       가격 (선택, 기본 무료)
     * @param userDetails 인증된 사용자
     * @return 업로드 결과
     */
    @PostMapping(value = "/upload/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ScenarioUploadResponse> uploadScenarioFromText(
            @RequestBody String content,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용이 비어있습니다.");
        }

        UUID authorId = UUID.fromString(userDetails.getUsername());
        Scenario scenario = uploadService.uploadFromMarkdown(authorId, content, price);

        return ResponseEntity.ok(ScenarioUploadResponse.from(scenario));
    }

    /**
     * 시나리오 유효성 검증 (업로드 전 미리보기)
     * 
     * @param file Markdown 파일
     * @return 유효성 검증 결과
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> validateScenario(
            @RequestParam("file") MultipartFile file) {

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            uploadService.validateMarkdown(content);
            return ResponseEntity.ok("유효한 시나리오 파일입니다.");
        } catch (IOException e) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        } catch (ScenarioParseException e) {
            throw e;
        }
    }

    /**
     * 시나리오 템플릿 다운로드
     * 
     * @return Markdown 템플릿 파일
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        Resource resource = new ClassPathResource("templates/scenario-template.md");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scenario-template.md\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(resource);
    }
}
