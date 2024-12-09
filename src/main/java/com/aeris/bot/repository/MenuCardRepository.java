package com.aeris.bot.repository;

import com.aeris.bot.model.MenuCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenuCardRepository extends JpaRepository<MenuCard, Long> {
    Optional<MenuCard> findByName(String name);
}