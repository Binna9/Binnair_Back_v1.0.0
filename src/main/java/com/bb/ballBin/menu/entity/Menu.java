package com.bb.ballBin.menu.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "menus")
public class Menu {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true)
    private String menuId;

    @Column(unique = true, nullable = false)
    private String menuName;
    private String menuUrl;
    private String menuIcon;
    private String menuDesc;
    private String upperMenuId;

    private int menuOrder;
    @Column(nullable = false)
    private int menuLevel;
    @Column(nullable = false)
    private boolean isActive;

    @ElementCollection
    private Set<String> roles;
    @ElementCollection
    private Set<String> permissions;
}
