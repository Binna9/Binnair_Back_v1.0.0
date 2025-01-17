package com.bb.ballBin.menu.repository;

import com.bb.ballBin.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, String> {
}
