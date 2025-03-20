package com.bb.ballBin.file.Controller;

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
    public ResponseEntity<String> uploadFiles(
            @ModelAttribute FileRequestDto fileRequestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        fileService.uploadFiles(fileRequestDto.getTargetType(), fileRequestDto.getTargetId(), files);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "파일 삭제")
    public ResponseEntity<Void> deleteFilesByTarget(@ModelAttribute FileRequestDto fileRequestDto) {

        fileService.deleteFilesByTarget(fileRequestDto.getTargetType(), fileRequestDto.getTargetId());

        return ResponseEntity.ok().build();
    }
}
