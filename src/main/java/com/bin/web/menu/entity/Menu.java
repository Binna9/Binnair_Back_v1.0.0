package com.bin.web.menu.entity;

import com.bin.web.common.convert.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Convert(converter = BooleanToYNConverter.class)
    private boolean isActive;
}
