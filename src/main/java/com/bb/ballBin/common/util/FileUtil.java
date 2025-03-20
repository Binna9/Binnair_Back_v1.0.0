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

    @Value("${file.user.upload-dir}")
    private String userUploadDir;

    @Value("${file.product.upload-dir}")
    private String productUploadDir;

    @Value("${file.board.upload-dir}")
    private String boardUploadDir;

    /**
     * 저장된 파일의 전체 경로 가져오기
     *
     * @param type 타입
     * @param relativePath 저장된 파일의 상대 경로
     * @return 파일 객체
     */
    public File getFilePath(String type, String relativePath) {
        String baseDir;
        if ("user".equals(type)) {
            baseDir = userUploadDir;
        } else if ("board".equals(type)) {
            baseDir = boardUploadDir;
        } else if ("product".equals(type)){
            baseDir = productUploadDir;
        } else {
            throw new IllegalArgumentException("타입이 존재하지 않습니다.");
        }
        return new File(Paths.get(baseDir, relativePath).toString());
    }

    /**
     * 파일 저장 (사용자 또는 제품 이미지 저장)
     *
     * @param type   타입
     * @param id     사용자 ID 또는 제품 ID
     * @param file   업로드할 파일
     * @return 저장된 파일의 상대 경로 (예: userId/uuid-filename.png 또는 productId/uuid-filename.png)
     */
    public String saveFile(String type, String id, MultipartFile file) {

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID가 null 또는 비어 있습니다.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        try {
            String baseDir;
            if ("user".equals(type)) {
                baseDir = userUploadDir;
            } else if ("product".equals(type)) {
                baseDir = productUploadDir;
            } else if ("board".equals(type)) {
                baseDir = boardUploadDir;
            } else {
                throw new IllegalArgumentException("잘못된 파일 유형: " + type);
            }

            String targetDir = Paths.get(baseDir, id).toString();
            File directory = new File(targetDir);

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

            return Paths.get(id, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생", e);
        }
    }

    /**
     * 파일 삭제 기능
     *
     * @param type "user", "product", "board" 등
     * @param relativePath 삭제할 파일의 상대 경로
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String type, String relativePath) {
        File file = getFilePath(type, relativePath);
        return file.exists() && file.delete();
    }

    /**
     * 디렉토리 삭제 기능
     *
     * @param type 타입 ("user", "product", "board" 등)
     * @param id   사용자 또는 제품 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteDirectory(String type, String id) {

        String baseDir;

        if ("user".equals(type)) {
            baseDir = userUploadDir;
        } else if ("product".equals(type)) {
            baseDir = productUploadDir;
        } else if ("board".equals(type)) {
            baseDir = boardUploadDir;
        } else {
            throw new IllegalArgumentException("잘못된 파일 유형: " + type);
        }

        File directory = new File(Paths.get(baseDir, id).toString());

        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(type, file.getName());
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    /**
     * 이미지 반환 (사용자 또는 제품)
     *
     * @param type 타입
     * @param relativePath 저장된 파일의 상대 경로
     * @return 이미지 파일을 ResponseEntity<Resource>로 반환
     */
    public ResponseEntity<Resource> getImageResponse(String type, String relativePath) {

        if (relativePath == null) {
            return ResponseEntity.notFound().build();
        }

        File imageFile = getFilePath(type, relativePath);

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
     * 파일 확장자를 기반으로 MIME 타입 반환(이미지 기준)
     */
    private MediaType getMediaType(File file) {
        String filename = file.getName().toLowerCase();

        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".jpeg") || filename.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (filename.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (filename.endsWith(".webp")) return MediaType.valueOf("image/webp");

        return MediaType.APPLICATION_OCTET_STREAM; // 기타 파일 타입
    }
}
