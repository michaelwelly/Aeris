package com.aeris.bot.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserOrderCache {

    private final Map<String, UUID> userOrderMap = new ConcurrentHashMap<>();

    // Сохраняем orderId для пользователя
    public void saveOrderId(String chatId, UUID orderId) {
        userOrderMap.put(chatId, orderId);
    }

    // Получаем orderId для пользователя
    public UUID getOrderId(String chatId) {
        return userOrderMap.get(chatId);
    }

    // Удаляем orderId для пользователя
    public void removeOrderId(String chatId) {
        userOrderMap.remove(chatId);
    }
}