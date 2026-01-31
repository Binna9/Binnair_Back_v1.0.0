package com.bin.web.board.entity;

import com.bin.web.common.entity.BaseEntity;
import com.bin.web.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "boards")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "board_id")
    private String boardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(nullable = false)
    private int views;
    @Column(nullable = false)
    private int likes;
    @Column(nullable = false)
    private int unlikes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", referencedColumnName = "user_id", nullable = false)
    private User writer;

    @Column(name = "writer_name", nullable = false)
    private String writerName;
}
