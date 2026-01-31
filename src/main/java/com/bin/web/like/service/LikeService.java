package com.bin.web.like.service;

import com.bin.web.board.entity.Board;
import com.bin.web.board.repository.BoardRepository;
import com.bin.web.common.exception.NotFoundException;
import com.bin.web.like.entity.Like;
import com.bin.web.like.entity.LikeStatus;
import com.bin.web.like.repository.LikeRepository;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional
    public void toggleLike(String userId, String boardId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Board not found"));

        Optional<Like> existingLike = likeRepository.findByUserAndBoard(user, board);

        if (existingLike.isPresent()) {
            Like like = existingLike.get();
            if (like.getStatus() == LikeStatus.LIKE) {
                likeRepository.delete(like);
                board.setLikes(board.getLikes() - 1);
            } else {
                like.setStatus(LikeStatus.LIKE);
                likeRepository.save(like);
                board.setLikes(board.getLikes() + 1);
                board.setUnlikes(board.getUnlikes() - 1);
            }
        } else {
            Like newLike = Like.builder()
                    .user(user)
                    .board(board)
                    .status(LikeStatus.LIKE)
                    .build();
            likeRepository.save(newLike);
            board.setLikes(board.getLikes() + 1);
        }

        boardRepository.save(board);
    }

    @Transactional
    public void toggleUnlike(String userId, String boardId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.notfound.user"));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Board not found"));

        Optional<Like> existingLike = likeRepository.findByUserAndBoard(user, board);

        if (existingLike.isPresent()) {
            Like like = existingLike.get();
            if (like.getStatus() == LikeStatus.UNLIKE) {
                likeRepository.delete(like);
                board.setUnlikes(board.getUnlikes() - 1);
            } else {
                like.setStatus(LikeStatus.UNLIKE);
                likeRepository.save(like);
                board.setLikes(board.getLikes() - 1);
                board.setUnlikes(board.getUnlikes() + 1);
            }
        } else {
            Like newUnlike = Like.builder()
                    .user(user)
                    .board(board)
                    .status(LikeStatus.UNLIKE)
                    .build();
            likeRepository.save(newUnlike);
            board.setUnlikes(board.getUnlikes() + 1);
        }

        boardRepository.save(board);
    }
}
