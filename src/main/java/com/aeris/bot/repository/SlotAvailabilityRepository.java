package com.aeris.bot.repository;

import com.aeris.bot.model.SlotAvailability;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotAvailabilityRepository extends JpaRepository<SlotAvailability, Long> {

    boolean existsByTableIdAndDateAndTimeSlot(Long tableId, LocalDate date, LocalTime timeSlot);
    List<SlotAvailability> findByDateAndStatus(LocalDate date, String status);
    List<SlotAvailability> findByDateAndTableId(LocalDate date, Long tableId);
    List<SlotAvailability> findByDateBetween(LocalDate startDate, LocalDate endDate);
    @Query("SELECT s FROM SlotAvailability s WHERE s.table.id = :tableId AND s.date = :date AND s.timeSlot = :timeSlot")
    Optional<SlotAvailability> findByTableIdAndDateAndTimeSlot(@Param("tableId") Long tableId, @Param("date") LocalDate date, @Param("timeSlot") LocalTime timeSlot);
}