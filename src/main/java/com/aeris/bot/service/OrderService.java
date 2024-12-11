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

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        RestaurantTableRepository restaurantTableRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    /**
     * Создаёт новый заказ.
     */
    public Order createOrder(Long userId, Long tableId, LocalDateTime bookingDateTime, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with ID: " + tableId));

        // Проверяем доступность стола
        if (!isTableAvailable(tableId, bookingDateTime)) {
            throw new IllegalStateException("Table is not available at the specified time.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTable(table);
        order.setBookingDateTime(bookingDateTime);
        order.setStatus("PENDING");
        order.setComment(comment);

        return orderRepository.save(order);
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
}