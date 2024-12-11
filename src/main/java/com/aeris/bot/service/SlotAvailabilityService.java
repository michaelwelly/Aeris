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

@Service
public class SlotAvailabilityService {

    private final SlotAvailabilityRepository slotAvailabilityRepository;

    public SlotAvailabilityService(SlotAvailabilityRepository slotAvailabilityRepository) {
        this.slotAvailabilityRepository = slotAvailabilityRepository;
    }
    /**
     * Получает слот по ID.
     */
    public SlotAvailability getSlotById(Long slotId) {
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
                if (!slotAvailabilityRepository.existsByTableIdAndDateAndTimeSlot(table.getId(), date, time)) {
                    SlotAvailability slot = new SlotAvailability();
                    slot.setDate(date);
                    slot.setTimeSlot(time);
                    slot.setTable(table);
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setCreatedBySystem(true); // Указываем, что слот создан системой
                    slot.setPrice(fixedPrice); // Устанавливаем фиксированную цену
                    slotAvailabilityRepository.save(slot);
                }
                time = time.plusHours(1);
            }
        }
    }
    /**
     * Проверка доступности слота.
     */
    public boolean isSlotAvailable(Long tableId, LocalDate date, LocalTime timeSlot) {
        return slotAvailabilityRepository.findByTableIdAndDateAndTimeSlot(tableId, date, timeSlot)
                .map(slot -> slot.getStatus().equals(SlotStatus.AVAILABLE))
                .orElse(false); // Если слот не найден, он недоступен
    }
    /**
     * Получение доступных слотов на дату.
     */
    public List<SlotAvailability> getAvailableSlots(LocalDate date) {
        return slotAvailabilityRepository.findByDateAndStatus(date, SlotStatus.AVAILABLE.name());
    }
    /**
     * Резервирование слота по идентификатору.
     */
    @Transactional
    public void reserveSlot(Long slotId) {
        SlotAvailability slot = slotAvailabilityRepository.findById(slotId)
                .orElseThrow(() -> new IllegalStateException("Слот не найден."));
        if (!slot.getStatus().equals(SlotStatus.AVAILABLE)) {
            throw new IllegalStateException("Слот уже занят.");
        }
        slot.setStatus(SlotStatus.RESERVED);
        slotAvailabilityRepository.save(slot);
    }
    /**
     * Резервирование слота по заказу.
     */
    @Transactional
    public void reserveSlot(Order order, LocalDate date, LocalTime timeSlot) {
        SlotAvailability slot = slotAvailabilityRepository.findByDateAndTableId(date, order.getTable().getId()).stream()
                .filter(s -> s.getTimeSlot().equals(timeSlot) && s.getStatus().equals(SlotStatus.AVAILABLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Слот недоступен."));
        slot.setStatus(SlotStatus.RESERVED);
        slot.setOrder(order);
        slotAvailabilityRepository.save(slot);
    }
    /**
     * Резервирование слота по таблице, дате и времени.
     */
    @Transactional
    public void reserveSlot(Long tableId, LocalDate date, LocalTime timeSlot) {
        SlotAvailability slot = slotAvailabilityRepository.findByTableIdAndDateAndTimeSlot(tableId, date, timeSlot)
                .orElseThrow(() -> new IllegalStateException("Слот не найден."));

        if (!slot.getStatus().equals(SlotStatus.AVAILABLE)) {
            throw new IllegalStateException("Слот уже занят.");
        }

        slot.setStatus(SlotStatus.RESERVED);
        slotAvailabilityRepository.save(slot);
    }
    /**
     * Освобождение слота.
     */
    @Transactional
    public void releaseSlot(Order order) {
        List<SlotAvailability> slots = slotAvailabilityRepository.findByDateAndTableId(
                order.getBookingDateTime().toLocalDate(), order.getTable().getId());
        for (SlotAvailability slot : slots) {
            if (slot.getOrder() != null && slot.getOrder().getId().equals(order.getId())) {
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setOrder(null);
                slotAvailabilityRepository.save(slot);
            }
        }
    }
    /**
     * Получение недельного расписания.
     */
    public List<SlotAvailability> getWeeklySchedule(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(7);
        return slotAvailabilityRepository.findByDateBetween(startDate, endDate);
    }
}