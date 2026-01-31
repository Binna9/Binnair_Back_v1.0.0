package com.bin.web.file.model;

import com.bin.web.file.entity.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileRequestDto {

    @Schema(description = "파일 타입")
    private TargetType targetType;
    @Schema(description = "파일 ID")
    private String targetId;
}
