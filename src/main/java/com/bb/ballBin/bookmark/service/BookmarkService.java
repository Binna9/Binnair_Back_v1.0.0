package com.bb.ballBin.bookmark.service;

import com.bb.ballBin.bookmark.entity.Bookmark;
import com.bb.ballBin.bookmark.mapper.BookmarkMapper;
import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import com.bb.ballBin.bookmark.repository.BookmarkRepository;
import com.bb.ballBin.cart.service.CartService;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final BookmarkMapper bookmarkMapper;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    /**
     * 즐겨찾기 목록 조회
     */
    @Transactional
    public Page<BookmarkResponseDto> allBookmarks(String userId, Pageable pageable) {
        return bookmarkRepository.findByUserUserId(userId, pageable)
                .map(bookmarkMapper::toDto);
    }

    /**
     * 즐겨찾기 추가
     */
    public void addBookmark(String userId, BookmarkRequestDto bookmarkRequestDto) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("error.user.notfound"));

            Bookmark bookmark = bookmarkMapper.toEntity(bookmarkRequestDto);
            bookmark.setUser(user);

            bookmarkRepository.save(bookmark);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }

    /**
     * 즐겨찾기 삭제
     */
    public void removeBookmark(String bookmarkId) {
        try {
            bookmarkRepository.deleteById(bookmarkId);
        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }
}
