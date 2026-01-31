package com.bin.web.bookmark.repository;

import com.bin.web.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    Page<Bookmark> findByUserUserId(String userId, Pageable pageable);
}
