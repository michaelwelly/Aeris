package com.aeris.bot.service;

import com.aeris.bot.model.*;
import com.aeris.bot.repository.OrderRepository;
import com.aeris.bot.repository.RestaurantTableRepository;
import com.aeris.bot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;
    private final SlotAvailabilityService slotAvailabilityService;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class); // Логгер

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
    public Order createOrder(UUID userId, UUID tableId, LocalDate bookingDate, LocalTime bookingTime, String comment) {
        if (bookingDate == null || bookingTime == null) {
            throw new IllegalStateException("Booking date and time cannot be null");
        }

        if (!slotAvailabilityService.isSlotAvailable(bookingDate, bookingTime)) {
            throw new IllegalStateException("Slot is not available");
        }

        // Проверяем пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Проверяем стол
        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with ID: " + tableId));

        // Создаем заказ
        Order order = new Order();
        order.setUser(user);
        order.setTable(table);
        order.setBookingDate(bookingDate);
        order.setBookingTime(bookingTime);
        order.setStatus("PENDING");
        order.setComment(comment);

        return orderRepository.save(order);
    }
    /**
     * Подтверждает заказ, обновляя его статус на CONFIRMED.
     */
    public Order confirmOrder(UUID orderId) {
        Order order = getOrderById(orderId);

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Order cannot be confirmed. Current status: " + order.getStatus());
        }

        order.setStatus("CONFIRMED");
        return orderRepository.save(order);
    }
    /**
     * Отменяет заказ.
     */
    public void cancelOrder(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        releaseResources(order);
    }
    /**
     * Удалить заказ.
     */
    public void deleteOrder(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            log.error("Попытка удалить заказ с несуществующим ID: {}", orderId);
            throw new EntityNotFoundException("Order not found with ID: " + orderId);
        }

        orderRepository.deleteById(orderId);
        log.info("Заказ с ID {} успешно удален.", orderId);
    }

    /**
     * Проверяет доступность стола на указанную дату и время.
     */
    public boolean isTableAvailable(UUID tableId, LocalDate bookingDate, LocalTime bookingTime) {
        List<Order> orders = orderRepository.findByTableIdAndBookingDateAndTime(tableId, bookingDate, bookingTime);
        return orders.isEmpty();
    }/**

     /**
     * Получает текущий активный заказ пользователя.
     *
     * @param userId ID пользователя.
     * @return Активный заказ пользователя.
     * @throws EntityNotFoundException если пользователь не найден.
     * @throws IllegalStateException если активный заказ отсутствует.
     */
    public Order getActiveOrderByUser(UUID userId) {
        // Получаем пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));

        // Ищем заказ со статусом "PENDING"
        return orderRepository.findByUserAndStatus(user, "PENDING")
                .orElseThrow(() -> new IllegalStateException("Активный заказ для пользователя с ID " + userId + " не найден."));
    }
    /**
     * Получает заказ по ID.
     */
    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));
    }
    /**
     * Получить все заказы.
     */
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        log.info("Получено {} заказов.", orders.size());
        return orders;
    }
    /**
     * Получение заказов за определённый период дат.
     */
    public List<Order> getOrdersForPeriod(LocalDate startDate, LocalDate endDate) {
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }
    /**
     * Получение заказов пользователя.
     */
    public List<Order> getOrdersByUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return orderRepository.findByUser(user);
    }
    public LocalDate getOrderDate(UUID orderId) {
        Order order = getOrderById(orderId); // Получаем заказ по ID
        LocalDate bookingDate = order.getBookingDate(); // Предполагается, что в модели Order есть поле bookingDate (тип LocalDate)

        if (bookingDate == null) {
            throw new IllegalStateException("Дата бронирования отсутствует для заказа с ID: " + orderId);
        }

        return bookingDate;
    }
    public LocalTime getOrderTime(UUID orderId) {
        // Получаем заказ по ID
        Order order = getOrderById(orderId);

        // Проверяем, установлено ли время бронирования
        if (order.getBookingTime() == null) {
            throw new IllegalStateException("Время бронирования не установлено для заказа с ID: " + orderId);
        }

        // Возвращаем время бронирования
        return order.getBookingTime();
    }
    public String getOrderZone(UUID orderId) {
        Order order = getOrderById(orderId); // Получаем заказ по ID
        RestaurantTable table = order.getTable(); // Получаем стол, связанный с заказом

        if (table == null || table.getZone() == null) {
            throw new IllegalStateException("Зона стола отсутствует для заказа с ID: " + orderId);
        }

        return table.getZone(); // Возвращаем зону стола
    }
    public UUID getOrderTableId(UUID orderId) {
        // Получаем заказ по ID
        Order order = getOrderById(orderId);

        // Проверяем, связан ли заказ со столом
        if (order.getTable() == null) {
            throw new IllegalStateException("Стол не установлен для заказа с ID: " + orderId);
        }

        // Возвращаем ID стола
        return order.getTable().getId();
    }
    /**
     * Обновить заказ.
     */
    public Order updateOrder(UUID orderId, Order updatedOrder) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Обновляем только те поля, которые заданы в updatedOrder
        if (updatedOrder.getBookingDate() != null) {
            existingOrder.setBookingDate(updatedOrder.getBookingDate());
        }
        if (updatedOrder.getBookingTime() != null) {
            existingOrder.setBookingTime(updatedOrder.getBookingTime());
        }
        if (updatedOrder.getStatus() != null) {
            existingOrder.setStatus(updatedOrder.getStatus());
        }
        if (updatedOrder.getComment() != null) {
            existingOrder.setComment(updatedOrder.getComment());
        }
        if (updatedOrder.getTable() != null) {
            existingOrder.setTable(updatedOrder.getTable());
        }

        Order savedOrder = orderRepository.save(existingOrder);
        log.info("Заказ с ID {} успешно обновлен.", orderId);
        return savedOrder;
    }
    public Order updateOrderDate(UUID orderId, LocalDate newDate) {
        // Получаем заказ по ID
        Order order = getOrderById(orderId);

        // Проверяем, не пустая ли дата
        if (newDate == null) {
            throw new IllegalArgumentException("Дата не может быть пустой.");
        }

        // Обновляем дату бронирования
        order.setBookingDate(newDate);

        // Сохраняем изменения в базе данных
        return orderRepository.save(order);
    }
    public Order updateOrderSlot(UUID orderId, LocalDate bookingDate, LocalTime bookingTime) {
        // Получаем существующий заказ
        Order order = getOrderById(orderId);

        // Проверяем, доступен ли слот
        if (!slotAvailabilityService.isSlotAvailable(bookingDate, bookingTime)) {
            throw new IllegalStateException("Слот недоступен для выбранной даты и времени: "
                    + bookingDate + " " + bookingTime);
        }

        // Устанавливаем новые дату и время бронирования
        order.setBookingDate(bookingDate);
        order.setBookingTime(bookingTime);

        // Резервируем слот
        slotAvailabilityService.reserveSlot(order, bookingDate, bookingTime);

        // Сохраняем обновленный заказ
        return orderRepository.save(order);
    }
    public Order updateOrderZone(UUID orderId, String newZone) {
        Order order = getOrderById(orderId);
        RestaurantTable table = restaurantTableRepository.findByZone(newZone)
                .orElseThrow(() -> new EntityNotFoundException("Zone not found: " + newZone));
        order.setTable(table);
        return orderRepository.save(order);
    }
    public Order updateOrderTable(UUID orderId, UUID newTableId) {
        Order order = getOrderById(orderId);
        RestaurantTable table = restaurantTableRepository.findById(newTableId)
                .orElseThrow(() -> new EntityNotFoundException("Table not found: " + newTableId));
        order.setTable(table);
        return orderRepository.save(order);
    }
    public Order updateOrderStatus(UUID orderId, String newStatus) {
        try {
            // Получаем заказ по ID
            Order order = getOrderById(orderId);

            // Обновляем статус заказа
            order.setStatus(newStatus);

            // Сохраняем изменения в базе данных
            Order updatedOrder = orderRepository.save(order);

            log.info("Статус заказа {} успешно обновлен на {}", orderId, newStatus);
            return updatedOrder;
        } catch (EntityNotFoundException e) {
            log.error("Ошибка: Заказ с ID {} не найден.", orderId, e);
            throw new IllegalStateException("Заказ с указанным ID не найден.");
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса заказа {}: {}", orderId, e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка при обновлении статуса заказа.");
        }
    }
    /**
     * Освобождает все зарезервированные ресурсы для заказа.
     */
    public void releaseResources(Order order) {
        // Освобождаем слот
        slotAvailabilityService.releaseSlot(order);

        // Дополнительная логика для освобождения ресурсов, если потребуется
        log.info("Все ресурсы освобождены для заказа ID: {}", order.getId());
    }
    /**
     * Создает пустой заказ для пользователя.
     *
     * @param userId ID пользователя.
     * @return Созданный заказ.
     */
    public Order createEmptyOrder(UUID userId) {
        // Проверяем наличие пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));

        // Создаем пустой заказ
        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING"); // Статус "PENDING" указывает, что заказ в процессе заполнения
        order.setBookingDate(null); // Дата бронирования пока не установлена
        order.setBookingTime(null); // Время бронирования пока не установлено
        order.setTable(null); // Стол не выбран
        order.setComment(null); // Комментарий не задан

        // Сохраняем заказ в базе данных
        Order savedOrder = orderRepository.save(order);
        log.info("Создан новый пустой заказ с ID {} для пользователя {}", savedOrder.getId(), userId);

        return savedOrder;
    }
    public List<SlotAvailability> getAvailableSlotsForDate(LocalDate date) {
        // Используем метод findByDateAndStatus через сервис
        return slotAvailabilityService.getAvailableSlots(date, SlotStatus.AVAILABLE);
    }

}