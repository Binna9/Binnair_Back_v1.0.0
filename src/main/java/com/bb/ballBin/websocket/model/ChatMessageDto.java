package com.bb.ballBin.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {

    @Schema(description = "세션 ID")
    private String id;
    @Schema(description = "사용자 명")
    private String sender;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "보낸 시간")
    private String timestamp;

}
