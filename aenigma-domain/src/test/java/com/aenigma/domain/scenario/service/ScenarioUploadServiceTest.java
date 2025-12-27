package com.aenigma.domain.scenario.service;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.scenario.parser.MarkdownScenarioParser;
import com.aenigma.domain.scenario.parser.ScenarioParseException;
import com.aenigma.domain.scenario.repository.ScenarioRepository;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ScenarioUploadService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ScenarioUploadServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private MarkdownScenarioParser markdownParser = new MarkdownScenarioParser();

    @InjectMocks
    private ScenarioUploadService uploadService;

    private User testAuthor;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        testAuthor = User.builder()
                .id(authorId)
                .username("GUEST_testauthor")
                .nickname("테스트작가")
                .build();
    }

    @Nested
    @DisplayName("시나리오 업로드")
    class UploadFromMarkdown {

        @Test
        @DisplayName("Markdown 파일로 시나리오를 생성한다")
        void uploadScenarioFromMarkdown() {
            // given
            String markdownContent = """
                    # 시나리오: 테스트 시나리오

                    ## 기본 정보
                    - 플레이어: [4~6]명
                    - 예상 시간: [120]분
                    - 난이도: [3]
                    - 태그: 미스터리

                    ## 배경 스토리
                    테스트 배경 스토리입니다.

                    ---

                    ## 역할

                    ### [CRIMINAL] 범인 캐릭터
                    - **설명**: 범인의 설명
                    - **비밀**: 범인의 비밀
                    - **목표**: 범인의 목표
                    - **관계**: 범인의 관계

                    ### [SUSPECT] 용의자 캐릭터
                    - **설명**: 용의자의 설명
                    - **비밀**: 용의자의 비밀
                    - **목표**: 용의자의 목표
                    - **관계**: 용의자의 관계

                    ---

                    ## 단서

                    ### [PUBLIC] 공개 단서
                    - **내용**: 공개 단서 내용
                    - **라운드**: 1
                    - **중요도**: 5
                    """;

            given(userRepository.findById(authorId)).willReturn(Optional.of(testAuthor));
            given(scenarioRepository.save(any(Scenario.class))).willAnswer(invocation -> {
                Scenario scenario = invocation.getArgument(0);
                // ID 설정 시뮬레이션
                return scenario;
            });

            // when
            Scenario result = uploadService.uploadFromMarkdown(authorId, markdownContent, BigDecimal.ZERO);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 시나리오");
            assertThat(result.getAuthor()).isEqualTo(testAuthor);
            assertThat(result.getMinPlayers()).isEqualTo(4);
            assertThat(result.getMaxPlayers()).isEqualTo(6);
            assertThat(result.getStatus()).isEqualTo(ScenarioStatus.DRAFT);

            // 역할 검증
            assertThat(result.getRoles()).hasSize(2);
            assertThat(result.getRoles().get(0).getRoleType()).isEqualTo(GameRole.CRIMINAL);
            assertThat(result.getRoles().get(1).getRoleType()).isEqualTo(GameRole.SUSPECT);

            // 단서 검증
            assertThat(result.getClues()).hasSize(1);
            assertThat(result.getClues().get(0).getClueType()).isEqualTo(ClueType.PUBLIC);

            // 저장 검증
            verify(scenarioRepository).save(any(Scenario.class));
        }

        @Test
        @DisplayName("유료 시나리오를 생성한다")
        void uploadPaidScenario() {
            // given
            String markdownContent = """
                    # 시나리오: 유료 시나리오

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;
            BigDecimal price = new BigDecimal("9900");

            given(userRepository.findById(authorId)).willReturn(Optional.of(testAuthor));
            given(scenarioRepository.save(any(Scenario.class))).willAnswer(i -> i.getArgument(0));

            // when
            Scenario result = uploadService.uploadFromMarkdown(authorId, markdownContent, price);

            // then
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("9900"));
            assertThat(result.isFree()).isFalse();
        }

        @Test
        @DisplayName("가격이 null이면 무료 시나리오로 생성한다")
        void uploadFreeScenarioWhenPriceIsNull() {
            // given
            String markdownContent = """
                    # 시나리오: 무료 시나리오

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 테스트
                    """;

            given(userRepository.findById(authorId)).willReturn(Optional.of(testAuthor));
            given(scenarioRepository.save(any(Scenario.class))).willAnswer(i -> i.getArgument(0));

            // when
            Scenario result = uploadService.uploadFromMarkdown(authorId, markdownContent, null);

            // then
            assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isFree()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외를 던진다")
        void throwExceptionForNonExistingUser() {
            // given
            UUID nonExistingId = UUID.randomUUID();
            String markdownContent = "# 시나리오: 테스트";

            given(userRepository.findById(nonExistingId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> uploadService.uploadFromMarkdown(nonExistingId, markdownContent, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("PERSONAL 단서가 역할에 연결된다")
        void personalClueLinkedToRole() {
            // given
            String markdownContent = """
                    # 시나리오: 테스트

                    ## 역할

                    ### [CRIMINAL] 범인 캐릭터
                    - **설명**: 범인

                    ### [DETECTIVE] 탐정 캐릭터
                    - **설명**: 탐정

                    ---

                    ## 단서

                    ### [PERSONAL:탐정 캐릭터] 개인 단서
                    - **내용**: 탐정만 알 수 있는 정보
                    - **라운드**: 1
                    """;

            given(userRepository.findById(authorId)).willReturn(Optional.of(testAuthor));
            given(scenarioRepository.save(any(Scenario.class))).willAnswer(i -> i.getArgument(0));

            // when
            Scenario result = uploadService.uploadFromMarkdown(authorId, markdownContent, null);

            // then
            ScenarioClue personalClue = result.getClues().get(0);
            assertThat(personalClue.getClueType()).isEqualTo(ClueType.PERSONAL);
            assertThat(personalClue.getAssignedRole()).isNotNull();
            assertThat(personalClue.getAssignedRole().getName()).isEqualTo("탐정 캐릭터");
        }

        @Test
        @DisplayName("탐정 역할이 있으면 hasDetective가 true이다")
        void hasDetectiveTrue() {
            // given
            String markdownContent = """
                    # 시나리오: 탐정 있는 시나리오

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 범인

                    ### [DETECTIVE] 탐정
                    - **설명**: 탐정
                    """;

            given(userRepository.findById(authorId)).willReturn(Optional.of(testAuthor));
            given(scenarioRepository.save(any(Scenario.class))).willAnswer(i -> i.getArgument(0));

            // when
            Scenario result = uploadService.uploadFromMarkdown(authorId, markdownContent, null);

            // then
            assertThat(result.isHasDetective()).isTrue();
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    class Validation {

        @Test
        @DisplayName("유효한 Markdown은 예외를 던지지 않는다")
        void validMarkdownPassesValidation() {
            // given
            String validContent = """
                    # 시나리오: 유효한 시나리오

                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 범인 설명
                    """;

            // when & then
            assertThatCode(() -> uploadService.validateMarkdown(validContent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("제목이 없으면 예외를 던진다")
        void throwExceptionForMissingTitle() {
            // given
            String invalidContent = """
                    ## 역할
                    ### [CRIMINAL] 범인
                    - **설명**: 범인
                    """;

            // when & then
            assertThatThrownBy(() -> uploadService.validateMarkdown(invalidContent))
                    .isInstanceOf(ScenarioParseException.class);
        }

        @Test
        @DisplayName("역할이 없으면 예외를 던진다")
        void throwExceptionForMissingRoles() {
            // given
            String invalidContent = """
                    # 시나리오: 역할 없는 시나리오

                    ## 단서
                    ### [PUBLIC] 단서
                    - **내용**: 내용
                    """;

            // when & then
            assertThatThrownBy(() -> uploadService.validateMarkdown(invalidContent))
                    .isInstanceOf(ScenarioParseException.class)
                    .hasMessageContaining("역할");
        }

        @Test
        @DisplayName("범인 역할이 없으면 예외를 던진다")
        void throwExceptionForMissingCriminal() {
            // given
            String invalidContent = """
                    # 시나리오: 범인 없는 시나리오

                    ## 역할
                    ### [DETECTIVE] 탐정
                    - **설명**: 탐정 설명

                    ### [SUSPECT] 용의자
                    - **설명**: 용의자 설명
                    """;

            // when & then
            assertThatThrownBy(() -> uploadService.validateMarkdown(invalidContent))
                    .isInstanceOf(ScenarioParseException.class)
                    .hasMessageContaining("범인");
        }
    }
}
