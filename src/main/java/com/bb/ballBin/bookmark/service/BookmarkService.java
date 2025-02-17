package com.bb.ballBin.bookmark.service;

import com.bb.ballBin.bookmark.entity.Bookmark;
import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import com.bb.ballBin.bookmark.repository.BookmarkRepository;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.repository.ProductRepository;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 특정 사용자의 즐겨찾기 목록 조회
     */
    public List<BookmarkResponseDto> getUserBookmarks(String userId) {
        return bookmarkRepository.findByUserUserId(userId).stream()
                .map(Bookmark::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 즐겨찾기 추가 (SecurityContext에서 userId 받아옴)
     */
    public BookmarkResponseDto addBookmark(String userId, BookmarkRequestDto bookmarkRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("error.user.notfound"));

        Product product = productRepository.findById(bookmarkRequestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .product(product)
                .build();

        bookmarkRepository.save(bookmark);
        return bookmark.toDto();
    }

    /**
     * 즐겨찾기 삭제
     */
    public void removeBookmark(String bookmarkId) {
        bookmarkRepository.deleteById(bookmarkId);
    }
}
