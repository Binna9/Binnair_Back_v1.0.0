package com.bb.ballBin.bookmark.repository;

import com.bb.ballBin.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    List<Bookmark> findByUserUserId(String userId);
}
