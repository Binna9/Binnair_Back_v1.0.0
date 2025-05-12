package com.bb.ballBin.websocket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {

    private String id;
    private String sender;
    private String content;
    private String timestamp;

}
