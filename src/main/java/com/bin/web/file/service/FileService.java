package com.bin.web.file.service;

import com.bin.web.common.exception.NotFoundException;
import com.bin.web.common.util.FileUtil;
import com.bin.web.file.entity.File;
import com.bin.web.file.entity.TargetType;
import com.bin.web.file.entity.FileType;
import com.bin.web.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;


import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {


    private final FileUtil fileUtil;
    private final FileRepository fileRepository;

    /**
     * 특정 TargetType + TargetId에 해당하는 파일 조회
     */
    public List<File> getFilesByTarget(TargetType targetType, String targetId) {
        return fileRepository.findByTargetIdAndTargetType(targetId, targetType);
    }

    /**
     * FIle DownLoad
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFiles(String fileId) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("error.file.notfound"));

        // filePath는 "targetId/uuid.ext" (relative)
        java.io.File diskFile = fileUtil.getFilePath(file.getTargetType().name().toLowerCase(), file.getFilePath());

        if (!diskFile.exists() || diskFile.isDirectory()) {
            throw new NotFoundException("error.file.notfound");
        }

        Resource resource = new FileSystemResource(diskFile);

        MediaType mediaType = fileUtil.getMediaType(diskFile);

        String encoded = UriUtils.encode(file.getOriginalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentLength(diskFile.length())
                .body(resource);
    }


    /**
     * 파일 업로드
     */
    public void uploadFiles(TargetType targetType, String targetId, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("error.file.notfound");
        }

        if (targetType == TargetType.USER) {
            deleteFilesByTarget(targetType, targetId);
        }

        for (MultipartFile file : files) {
            String savedPath = fileUtil.saveFile(targetType.name().toLowerCase(), targetId, file);
            saveFileMetadata(targetType, targetId, savedPath, file);
        }
    }

    /**
     * 파일 메타데이터 저장
     */
    private void saveFileMetadata(TargetType targetType, String targetId, String filePath, MultipartFile file) {

        File fileEntity = File.builder()
                .targetId(targetId)
                .targetType(targetType)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .fileType(FileType.fromMimeType( file.getContentType() == null ? "" : file.getContentType()))
                .originalFileName(file.getOriginalFilename())
                .build();

        fileRepository.save(fileEntity);
    }

    /**
     * 특정 TargetType + TargetId에 해당하는 모든 파일 삭제
     */
    @Transactional
    public void deleteFilesByTarget(TargetType targetType, String targetId) {

        List<File> files = getFilesByTarget(targetType, targetId);
        if (files.isEmpty()) return;

        for (File f : files) {
            boolean deleted = fileUtil.deleteFile(
                    targetType.name().toLowerCase(),
                    f.getFilePath()
            );
            if (!deleted) {
                throw new RuntimeException("파일 삭제 실패: " + f.getFilePath());
            }
        }

        List<String> ids = files.stream().map(File::getFileId).toList();
        fileRepository.deleteAllByIdInBatch(ids);

        if (!fileRepository.existsByTargetTypeAndTargetId(targetType, targetId)) {
            boolean dirDeleted = fileUtil.deleteDirectory(targetType.name().toLowerCase(), targetId);
            if (!dirDeleted) {
                throw new RuntimeException("디렉토리 삭제 실패: " + targetId);
            }
        }
    }

    @Transactional
    public void deleteFiles(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;

        List<String> uniqIds = fileIds.stream().distinct().toList();

        List<File> files = fileRepository.findAllById(uniqIds);

        if (files.size() != uniqIds.size()) {
            throw new NotFoundException("error.file.notfound");
        }

        Map<TargetKey, List<File>> byTarget = files.stream()
                .collect(Collectors.groupingBy(f -> new TargetKey(f.getTargetType(), f.getTargetId())));

        for (File f : files) {
            boolean deleted = fileUtil.deleteFile(
                    f.getTargetType().name().toLowerCase(),
                    f.getFilePath()
            );
            if (!deleted) {
                throw new RuntimeException("파일 삭제 실패: " + f.getFilePath());
            }
        }

        fileRepository.deleteAllByIdInBatch(uniqIds);

        for (TargetKey key : byTarget.keySet()) {
            boolean exists = fileRepository.existsByTargetTypeAndTargetId(key.targetType(), key.targetId());
            if (!exists) {
                boolean dirDeleted = fileUtil.deleteDirectory(key.typeString(), key.targetId());
                if (!dirDeleted) {
                    throw new RuntimeException("디렉 삭제 실패: " + key.targetId());
                }
            }
        }
    }

    private record TargetKey(TargetType targetType, String targetId) {
        String typeString() { return targetType.name().toLowerCase(); }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }
}
