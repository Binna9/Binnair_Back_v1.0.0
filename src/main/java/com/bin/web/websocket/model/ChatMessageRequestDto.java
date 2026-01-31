package com.bin.web.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDto {

    @Schema(description = "세션 ID")
    private String id;
    @Schema(description = "사용자 명")
    private String sender;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "보낸 시간")
    private String timestamp;

}
