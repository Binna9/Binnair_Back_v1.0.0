package com.bb.ballBin.file.Controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.file.model.FileRequestDto;
import com.bb.ballBin.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "파일 업로드")
    @MessageKey(value = "success.file.upload")
    public ResponseEntity<Void> uploadFiles(
            @ModelAttribute FileRequestDto fileRequestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        fileService.uploadFiles(fileRequestDto.getTargetType(), fileRequestDto.getTargetId(), files);

        return ResponseEntity.ok().build();
    }
}
