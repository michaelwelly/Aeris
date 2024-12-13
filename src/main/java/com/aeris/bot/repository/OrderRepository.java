package com.aeris.bot.repository;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserId(UUID userId);

    @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.bookingDate = :bookingDate AND o.bookingTime = :bookingTime")
    List<Order> findByTableIdAndBookingDateAndTime(@Param("tableId") UUID tableId,
                                                   @Param("bookingDate") LocalDate bookingDate,
                                                   @Param("bookingTime") LocalTime bookingTime);

    @Query("SELECT o FROM Order o WHERE o.bookingDate = :bookingDate")
    List<Order> findByBookingDate(@Param("bookingDate") LocalDate bookingDate);

    @Query("SELECT o FROM Order o WHERE o.bookingDate = :bookingDate AND o.bookingTime BETWEEN :startTime AND :endTime")
    List<Order> findOrdersByDateAndTimeRange(@Param("bookingDate") LocalDate bookingDate,
                                             @Param("startTime") LocalTime startTime,
                                             @Param("endTime") LocalTime endTime);

    @Query("SELECT o FROM Order o WHERE o.bookingDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    List<Order> findByUser(User user);

    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.status = :status")
    Optional<Order> findByUserAndStatus(@Param("user") User user, @Param("status") String status);
}