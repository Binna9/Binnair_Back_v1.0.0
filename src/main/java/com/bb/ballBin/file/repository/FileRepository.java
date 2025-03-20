package com.bb.ballBin.file.repository;

import com.bb.ballBin.file.entity.File;
import com.bb.ballBin.file.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, String> {

    List<File> findByTargetIdAndTargetType(String targetId, TargetType targetType);

    void deleteByTargetIdAndTargetType(String targetId, TargetType targetType);

}
