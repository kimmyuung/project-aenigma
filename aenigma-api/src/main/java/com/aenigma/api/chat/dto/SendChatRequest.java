package com.aenigma.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SendChatRequest {

    @NotBlank(message = "메시지 내용을 입력해주세요.")
    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다.")
    private String content;

    /**
     * 귓속말 수신자 ID (귓속말인 경우에만 필수)
     */
    private UUID receiverId;

    /**
     * 귓속말 여부
     */
    private boolean whisper = false;
}
