package com.aeris.bot.service;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.model.SlotAvailability;
import com.aeris.bot.model.SlotStatus;
import com.aeris.bot.repository.SlotAvailabilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SlotAvailabilityService {

    private final SlotAvailabilityRepository slotAvailabilityRepository;

    public SlotAvailabilityService(SlotAvailabilityRepository slotAvailabilityRepository) {
        this.slotAvailabilityRepository = slotAvailabilityRepository;
    }

    /**
     * Получает слот по ID.
     */
    public SlotAvailability getSlotById(UUID slotId) {
        return slotAvailabilityRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Slot not found with ID: " + slotId));
    }

    /**
     * Генерация слотов на заданную дату с фиксированной ценой.
     */
    @Transactional
    public void generateSlotsForDate(LocalDate date, List<RestaurantTable> tables, LocalTime startTime, LocalTime endTime, BigDecimal fixedPrice) {
        for (RestaurantTable table : tables) {
            LocalTime time = startTime;
            while (time.isBefore(endTime)) {
                Optional<SlotAvailability> existingSlot = slotAvailabilityRepository.findByTableIdAndDateAndTimeSlot(table.getId(), date, time);
                if (existingSlot.isEmpty() || existingSlot.get().getStatus() == SlotStatus.REMOVED) {
                    SlotAvailability slot = new SlotAvailability();
                    slot.setDate(date);
                    slot.setTimeSlot(time);
                    slot.setTable(table);
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setCreatedBySystem(true);
                    slot.setPrice(fixedPrice);
                    slotAvailabilityRepository.save(slot);
                }
                time = time.plusHours(1);
            }
        }
    }

    /**
     * Проверка доступности слота.
     */
    public boolean isSlotAvailable(UUID tableId, LocalDate date, LocalTime timeSlot) {
        return slotAvailabilityRepository.findByTableIdAndDateAndTimeSlot(tableId, date, timeSlot)
                .map(slot -> slot.getStatus() == SlotStatus.AVAILABLE)
                .orElse(false);
    }

    /**
     * Получение доступных слотов на дату.
     */
    public List<SlotAvailability> getAvailableSlots(LocalDate date) {
        return slotAvailabilityRepository.findByDateAndStatus(date, SlotStatus.AVAILABLE.name());
    }

    /**
     * Резервирование слота по ID.
     */
    @Transactional
    public void reserveSlot(UUID slotId) {
        SlotAvailability slot = getSlotById(slotId);
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Slot is not available.");
        }
        slot.setStatus(SlotStatus.RESERVED);
        slotAvailabilityRepository.save(slot);
    }

    /**
     * Резервирование слота по заказу.
     */
    @Transactional
    public void reserveSlot(Order order, LocalDate date, LocalTime timeSlot) {
        SlotAvailability slot = slotAvailabilityRepository.findByTableIdAndDateAndTimeSlot(order.getTable().getId(), date, timeSlot)
                .orElseThrow(() -> new IllegalStateException("Slot not available."));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Slot is already reserved.");
        }
        slot.setStatus(SlotStatus.RESERVED);
        slot.setOrder(order);
        slotAvailabilityRepository.save(slot);
    }

    /**
     * Освобождение слота.
     */
    @Transactional
    public void releaseSlot(Order order) {
        List<SlotAvailability> slots = slotAvailabilityRepository.findByDateAndTableId(order.getBookingDate(), order.getTable().getId());
        for (SlotAvailability slot : slots) {
            if (slot.getOrder() != null && slot.getOrder().getId().equals(order.getId())) {
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setOrder(null);
                slotAvailabilityRepository.save(slot);
            }
        }
    }

    /**
     * Получение расписания на неделю.
     */
    public List<SlotAvailability> getWeeklySchedule(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(7);
        return slotAvailabilityRepository.findByDateBetween(startDate, endDate);
    }
}