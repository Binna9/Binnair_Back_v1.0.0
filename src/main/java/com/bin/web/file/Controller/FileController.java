package com.bin.web.file.Controller;

import com.bin.web.common.annotation.MessageKey;
import com.bin.web.file.model.FileRequestDto;
import com.bin.web.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/{fileId}")
    @PreAuthorize("hasAuthority('FILE_DOWNLOAD')")
    @Operation(summary = "파일 다운로드")
    @MessageKey(value = "success.file.download")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        return fileService.downloadFiles(fileId);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('FILE_UPLOAD')")
    @Operation(summary = "파일 다중 업로드")
    @MessageKey(value = "success.file.upload")
    public ResponseEntity<Void> upload(
            @ModelAttribute FileRequestDto fileRequestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        fileService.uploadFiles(fileRequestDto.getTargetType(), fileRequestDto.getTargetId(), files);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('FILE_DELETE')")
    @Operation(summary = "파일 다중 삭제")
    @MessageKey("success.file.delete")
    public ResponseEntity<Void> remove(@RequestBody List<String> fileIds) {

        fileService.deleteFiles(fileIds);

        return ResponseEntity.ok().build();
    }
}
