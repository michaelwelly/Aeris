package com.aeris.bot.repository;

import com.aeris.bot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByTableIdAndBookingDateTime(Long tableId, LocalDateTime bookingDateTime);

    // Добавьте методы, если потребуется
}