package com.bb.ballBin.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardRequestDto {
    private String title;
    private String content;
    private String author;
}
