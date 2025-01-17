package com.bb.ballBin.bookmark.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true)
    private String bookmarkId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String objectId;

    @Column(nullable = false)
    private String objectType;

}
