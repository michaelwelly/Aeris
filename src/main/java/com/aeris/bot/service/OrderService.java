package com.aeris.bot.service;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.model.User;
import com.aeris.bot.repository.OrderRepository;
import com.aeris.bot.repository.RestaurantTableRepository;
import com.aeris.bot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;
    private final SlotAvailabilityService slotAvailabilityService;

    public OrderService(UserRepository userRepository,
                        RestaurantTableRepository restaurantTableRepository,
                        OrderRepository orderRepository,
                        SlotAvailabilityService slotAvailabilityService) {
        this.userRepository = userRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.orderRepository = orderRepository;
        this.slotAvailabilityService = slotAvailabilityService;
    }


    /**
     * Создаёт новый заказ и сохраняет его.
     */
    public Order createOrder(Long userId, Long tableId, LocalDateTime bookingDateTime, String comment) {
        // Здесь уже используется метод проверки доступности
        if (!slotAvailabilityService.isSlotAvailable(tableId, bookingDateTime.toLocalDate(), bookingDateTime.toLocalTime())) {
            throw new IllegalStateException("Slot is not available");
        }

        Order order = new Order();
        order.setUser(userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found")));
        order.setTable(restaurantTableRepository.findById(tableId).orElseThrow(() -> new EntityNotFoundException("Table not found")));
        order.setBookingDateTime(bookingDateTime);
        order.setStatus("PENDING");
        order.setComment(comment);

        Order savedOrder = orderRepository.save(order);
        slotAvailabilityService.reserveSlot(savedOrder, bookingDateTime.toLocalDate(), bookingDateTime.toLocalTime());
        return savedOrder;
    }

    /**
     * Отменяет заказ.
     */
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Обновляем статус заказа
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // Освобождаем зарезервированные слоты
        slotAvailabilityService.releaseSlot(order);
    }

    /**
     * Получение заказов за определённый период.
     */
    public List<Order> getOrdersForPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return orderRepository.findOrdersByDateRange(startDateTime, endDateTime);
    }

    /**
     * Получение заказов пользователя.
     */
    public List<Order> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return orderRepository.findByUser(user);
    }

    /**
     * Получает заказ по ID.
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));
    }

    /**
     * Получает все заказы пользователя.
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Получает все заказы.
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Обновляет статус заказа.
     */
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    /**
     * Обновляет заказ.
     */
    public Order updateOrder(Long id, Order updatedOrder) {
        Order existingOrder = getOrderById(id);

        // Обновляем поля заказа
        if (updatedOrder.getBookingDateTime() != null) {
            existingOrder.setBookingDateTime(updatedOrder.getBookingDateTime());
        }
        if (updatedOrder.getTable() != null) {
            existingOrder.setTable(updatedOrder.getTable());
        }
        if (updatedOrder.getStatus() != null) {
            existingOrder.setStatus(updatedOrder.getStatus());
        }
        if (updatedOrder.getComment() != null) {
            existingOrder.setComment(updatedOrder.getComment());
        }

        return orderRepository.save(existingOrder);
    }

    /**
     * Удаляет заказ по ID.
     */
    public void deleteOrder(Long orderId) {
        Order order = getOrderById(orderId);
        orderRepository.delete(order);
    }

    /**
     * Проверяет доступность стола на указанное время.
     */
    public boolean isTableAvailable(Long tableId, LocalDateTime bookingDateTime) {
        List<Order> orders = orderRepository.findByTableIdAndBookingDateTime(tableId, bookingDateTime);
        return orders.isEmpty();
    }
    /**
     * Обновляет дату заказа.
     */
    public Order updateOrderDate(Long orderId, LocalDateTime newDate) {
        Order order = getOrderById(orderId);
        order.setBookingDateTime(newDate);
        return orderRepository.save(order);
    }

    /**
     * Обновляет зону заказа.
     */
    public Order updateOrderZone(Long orderId, String newZone) {
        Order order = getOrderById(orderId);
        RestaurantTable table = restaurantTableRepository.findByZone(newZone)
                .orElseThrow(() -> new EntityNotFoundException("Zone not found: " + newZone));
        order.setTable(table);
        return orderRepository.save(order);
    }

    /**
     * Обновляет стол заказа.
     */
    public Order updateOrderTable(Long orderId, Long newTableId) {
        Order order = getOrderById(orderId);
        RestaurantTable table = restaurantTableRepository.findById(newTableId)
                .orElseThrow(() -> new EntityNotFoundException("Table not found: " + newTableId));
        order.setTable(table);
        return orderRepository.save(order);
    }

    /**
     * Обновляет временной слот заказа.
     */
    public Order updateOrderSlot(Long orderId, LocalDateTime newSlot) {
        Order order = getOrderById(orderId);
        if (!slotAvailabilityService.isSlotAvailable(order.getTable().getId(), newSlot.toLocalDate(), newSlot.toLocalTime())) {
            throw new IllegalStateException("Slot is not available");
        }
        order.setBookingDateTime(newSlot);
        slotAvailabilityService.reserveSlot(order, newSlot.toLocalDate(), newSlot.toLocalTime());
        return orderRepository.save(order);
    }

    /**
     * Получает дату и время заказа.
     */
    public LocalDateTime getOrderDateTime(Long orderId) {
        return getOrderById(orderId).getBookingDateTime();
    }

    /**
     * Получает зону стола заказа.
     */
    public String getOrderZone(Long orderId) {
        return getOrderById(orderId).getTable().getZone();
    }
}