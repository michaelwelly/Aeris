package com.aeris.bot.repository;

import com.aeris.bot.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    List<RestaurantTable> findByZoneAndAvailable(String zone, Boolean available);
    Optional<RestaurantTable> findByZone(String zone);

}