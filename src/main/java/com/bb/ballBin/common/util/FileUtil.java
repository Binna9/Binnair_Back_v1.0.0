package com.bb.ballBin.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 파일 저장 (사용자 ID 기반 폴더에 저장)
     *
     * @param file   업로드할 파일
     * @return 저장된 파일의 상대 경로 (예: userId/uuid-filename.png)
     */
    public String saveFile(String userId, MultipartFile file) {

        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId가 null 또는 비어 있습니다.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        try {
            String userDir = Paths.get(uploadDir, userId).toString();
            File directory = new File(userDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("파일 이름이 없습니다.");
            }

            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + extension;
            File destination = new File(directory, fileName);
            file.transferTo(destination);

            return Paths.get(userId, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생", e);
        }
    }


    /**
     * 저장된 파일의 전체 경로 가져오기
     *
     * @param relativePath 저장된 파일의 상대 경로 (예: userId/uuid-filename.png)
     * @return 파일 객체
     */
    public File getFilePath(String relativePath) {
        return new File(Paths.get(uploadDir, relativePath).toString());
    }

    /**
     * 파일 삭제 기능
     *
     * @param relativePath 삭제할 파일의 상대 경로
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String relativePath) {

        File file = new File(Paths.get(uploadDir, relativePath).toString());

        return file.exists() && file.delete();
    }

    /**
     * 프로필 이미지 반환 (컨트롤러에서 호출)
     *
     * @param relativePath 저장된 파일의 상대 경로
     * @return 이미지 파일을 ResponseEntity<Resource>로 반환
     */
    public ResponseEntity<Resource> getProfileImageResponse(String relativePath) {
        if (relativePath == null) {
            return ResponseEntity.notFound().build();
        }

        File imageFile = getFilePath(relativePath);

        if (!imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imageFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageFile.getName() + "\"")
                .contentType(getMediaType(imageFile))
                .body(resource);
    }

    /**
     * 파일 확장자를 기반으로 MIME 타입 반환
     */
    private MediaType getMediaType(File file) {
        String filename = file.getName().toLowerCase();

        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".jpeg") || filename.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (filename.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (filename.endsWith(".webp")) return MediaType.valueOf("image/webp");

        return null; // ❌ 잘못된 파일 형식이면 null 반환
    }
}
