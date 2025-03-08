package com.bb.ballBin.board.domain;

import java.util.Arrays;

public enum BoardType {

    NOTICE("공지사항"),
    FAQ("자주 묻는 질문"),
    FREE("자유게시판"),
    SUGGESTION("문의하기");

    private final String description;

    BoardType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * ✅ 유효한 boardType 인지 체크하는 메서드
     */
    public static boolean isValidType(String type) {
        return Arrays.stream(BoardType.values())
                .anyMatch(t -> t.name().equalsIgnoreCase(type));
    }
}
