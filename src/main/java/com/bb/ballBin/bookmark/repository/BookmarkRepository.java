package com.bb.ballBin.bookmark.repository;

import com.bb.ballBin.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, String> {

}
