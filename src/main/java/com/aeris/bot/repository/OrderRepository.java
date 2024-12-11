package com.aeris.bot.repository;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByTableIdAndBookingDateTime(Long tableId, LocalDateTime bookingDateTime);
    @Query("SELECT o FROM Order o WHERE o.bookingDateTime BETWEEN :startDateTime AND :endDateTime")
    List<Order> findOrdersByDateRange(@Param("startDateTime") LocalDateTime startDateTime,
                                      @Param("endDateTime") LocalDateTime endDateTime);
    List<Order> findByUser(User user);
}