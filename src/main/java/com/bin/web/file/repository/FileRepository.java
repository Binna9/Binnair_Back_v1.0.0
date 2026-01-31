package com.bin.web.file.repository;

import com.bin.web.file.entity.File;
import com.bin.web.file.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, String> {

    List<File> findByTargetIdAndTargetType(String targetId, TargetType targetType);

    List<File> findByTargetTypeAndTargetIdIn(TargetType targetType, List<String> targetIds);

    boolean existsByTargetTypeAndTargetId(TargetType targetType, String targetId);

    void deleteAllByIdInBatch(Iterable<String> ids);
}
