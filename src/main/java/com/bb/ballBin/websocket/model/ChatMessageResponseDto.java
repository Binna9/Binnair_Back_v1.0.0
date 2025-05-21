package com.bb.ballBin.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {

    @Schema(description = "사용자 명")
    private String sender;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "보낸 시간")
    private String timestamp;
}
