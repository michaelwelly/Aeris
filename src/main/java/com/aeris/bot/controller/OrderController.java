package com.aeris.bot.controller;

import com.aeris.bot.model.Order;
import com.aeris.bot.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        try {
            UUID userId = order.getUser().getId();
            UUID tableId = order.getTable().getId();
            LocalDate bookingDate = order.getBookingDate();
            LocalTime bookingTime = order.getBookingTime();
            String comment = order.getComment();

            Order createdOrder = orderService.createOrder(userId, tableId, bookingDate, bookingTime, comment);
            log.info("Заказ успешно создан: {}", createdOrder);
            return ResponseEntity.ok(createdOrder);
        } catch (IllegalStateException | EntityNotFoundException e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        try {
            Order order = orderService.getOrderById(id);
            log.info("Заказ найден: {}", order);
            return ResponseEntity.ok(order);
        } catch (EntityNotFoundException e) {
            log.error("Заказ с ID {} не найден: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        log.info("Получено {} заказов", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable UUID userId) {
        try {
            List<Order> orders = orderService.getOrdersByUser(userId);
            log.info("Получено {} заказов для пользователя {}", orders.size(), userId);
            return ResponseEntity.ok(orders);
        } catch (EntityNotFoundException e) {
            log.error("Ошибка при получении заказов пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable UUID id, @RequestBody Order updatedOrder) {
        try {
            Order order = orderService.updateOrder(id, updatedOrder);
            log.info("Заказ обновлен: {}", order);
            return ResponseEntity.ok(order);
        } catch (EntityNotFoundException e) {
            log.error("Ошибка при обновлении заказа с ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            Order order = orderService.updateOrderStatus(id, status);
            log.info("Статус заказа {} обновлен на {}", id, status);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Ошибка при обновлении статуса заказа {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        try {
            orderService.deleteOrder(id);
            log.info("Заказ с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.error("Ошибка при удалении заказа с ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/availability")
    public ResponseEntity<String> checkTableAvailability(@RequestParam UUID tableId,
                                                         @RequestParam LocalDate date,
                                                         @RequestParam LocalTime time) {
        boolean isAvailable = orderService.isTableAvailable(tableId, date, time);
        String message = isAvailable ? "Стол доступен для бронирования." : "Стол занят.";
        log.info("Доступность стола {} на {} {}: {}", tableId, date, time, message);
        return ResponseEntity.ok(message);
    }
}