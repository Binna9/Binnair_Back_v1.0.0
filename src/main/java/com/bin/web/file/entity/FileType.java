package com.bin.web.file.entity;

public enum FileType {
    IMAGE,      // 이미지 파일 (jpg, png, gif 등)
    DOCUMENT,   // 문서 파일 (pdf, doc, xls 등)
    VIDEO,      // 동영상 파일 (mp4, avi 등)
    AUDIO,      // 오디오 파일 (mp3, wav 등)
    OTHER;      // 기타 파일 (압축 파일, 실행 파일 등)

    public static FileType fromMimeType(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return IMAGE;
        } else if (mimeType.startsWith("application/pdf") || mimeType.startsWith("application/msword") ||
                mimeType.startsWith("application/vnd.ms-excel") || mimeType.startsWith("application/vnd.openxmlformats-officedocument")) {
            return DOCUMENT;
        } else if (mimeType.startsWith("video/")) {
            return VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return AUDIO;
        } else {
            return OTHER;
        }
    }
}


