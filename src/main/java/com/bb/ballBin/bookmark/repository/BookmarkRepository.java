package com.bb.ballBin.bookmark.repository;

import com.bb.ballBin.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    Page<Bookmark> findByUserUserId(String userId, Pageable pageable);
}
