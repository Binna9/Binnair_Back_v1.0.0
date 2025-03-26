package com.bb.ballBin.file.service;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.file.entity.File;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.entity.FileType;
import com.bb.ballBin.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
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
     * 파일 업로드
     */
    public void uploadFiles(TargetType targetType, String targetId, List<MultipartFile> files) {

        List<File> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String savedPath = fileUtil.saveFile(targetType.name().toLowerCase(), targetId, file); // 소문자로 변환
            File fileEntity = saveFileMetadata(targetType, targetId, savedPath, file);

            savedFiles.add(fileEntity);
        }
    }

    public void createFiles(TargetType targetType, List<MultipartFile> files){
        List<File> savedFiles = new ArrayList<>();

    }

    /**
     * 파일 메타데이터 저장
     */
    private File saveFileMetadata(TargetType targetType, String targetId, String filePath, MultipartFile file) {

        File fileEntity = File.builder()
                .targetId(targetId)
                .targetType(targetType)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .fileType(FileType.fromMimeType(file.getContentType()))
                .originalFileName(file.getOriginalFilename())
                .build();

        return fileRepository.save(fileEntity);
    }

    /**
     * 특정 TargetType + TargetId에 해당하는 모든 파일 삭제
     */
    public void deleteFilesByTarget(TargetType targetType, String targetId) {

        List<File> files = getFilesByTarget(targetType, targetId);

        for (File file : files) {
            boolean isDeleted = fileUtil.deleteFile(targetType.name().toLowerCase(), file.getFilePath());
            if (isDeleted) {
                fileRepository.delete(file);
            }
        }

        fileUtil.deleteDirectory(targetType.name().toLowerCase(), targetId);
    }

    /**
     * 파일 단일 삭제
     */
    public void deleteFile(String fileId){
        try {
            fileRepository.deleteById(fileId);
        } catch (Exception e) {
            throw new RuntimeException("삭제 중 오류 발생", e);
        }
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
