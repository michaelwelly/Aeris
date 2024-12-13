package com.aeris.bot.service;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.repository.OrderRepository;
import com.aeris.bot.repository.RestaurantTableRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantTableService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class); // Логгер

    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;
    private final SlotAvailabilityService slotAvailabilityService;

    public RestaurantTableService(RestaurantTableRepository restaurantTableRepository, OrderRepository orderRepository, SlotAvailabilityService slotAvailabilityService) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.orderRepository = orderRepository;
        this.slotAvailabilityService = slotAvailabilityService;
    }

    /**
     * Получить стол по ID.
     */
    public RestaurantTable getTableById(UUID id) {
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with ID: " + id));
    }

    /**
     * Получить список всех столов.
     */
    public List<RestaurantTable> getAllTables() {
        return restaurantTableRepository.findAll();
    }

    /**
     * Сохранить стол в базе данных.
     */
    public RestaurantTable saveTable(RestaurantTable table) {
        return restaurantTableRepository.save(table);
    }

    /**
     * Удалить стол по ID.
     */
    public void deleteTable(UUID id) {
        restaurantTableRepository.deleteById(id);
    }

    /**
     * Получить доступные столы в указанной зоне.
     */
    public List<RestaurantTable> getAvailableTablesByZone(String zoneName) {
        return restaurantTableRepository.findByZoneAndAvailable(zoneName, true);
    }

    /**
     * Проверяет доступность стола на указанную дату и время.
     *
     * @param tableId      UUID стола.
     * @param bookingDate  Дата бронирования.
     * @param bookingTime  Время бронирования.
     * @return true, если стол доступен; false, если стол уже забронирован.
     */
    public boolean isTableAvailable(UUID tableId, LocalDate bookingDate, LocalTime bookingTime) {
        // Шаг 1: Проверяем наличие конфликтующих заказов в базе
        List<Order> conflictingOrders = orderRepository.findByTableIdAndBookingDateAndTime(tableId, bookingDate, bookingTime);

        // Учитываем только заказы со статусами "CONFIRMED" или "PENDING"
        boolean hasActiveOrders = conflictingOrders.stream()
                .anyMatch(order -> "CONFIRMED".equals(order.getStatus()) || "PENDING".equals(order.getStatus()));

        if (hasActiveOrders) {
            log.info("Стол {} занят на дату {} и время {} из-за активных заказов.", tableId, bookingDate, bookingTime);
            return false; // Стол недоступен
        }

        // Шаг 2: Проверяем доступность слота через SlotAvailabilityService
        boolean slotAvailable = slotAvailabilityService.isSlotAvailable(bookingDate, bookingTime);

        if (!slotAvailable) {
            log.info("Слот для стола {} недоступен на дату {} и время {}.", tableId, bookingDate, bookingTime);
        }

        // Стол доступен только если нет активных заказов и слот свободен
        return slotAvailable;
    }
    /**
     * Резервирует стол для указанной даты и времени.
     */
    public void reserveTable(UUID tableId, LocalDate bookingDate, LocalTime bookingTime) {
        if (!isTableAvailable(tableId, bookingDate, bookingTime)) {
            throw new IllegalStateException("Table is not available for the given date and time.");
        }

        log.info("Стол {} успешно зарезервирован на {} {}", tableId, bookingDate, bookingTime);
    }

}