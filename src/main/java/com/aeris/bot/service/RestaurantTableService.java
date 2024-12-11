package com.aeris.bot.service;

import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.repository.RestaurantTableRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;

    public RestaurantTableService(RestaurantTableRepository restaurantTableRepository) {
        this.restaurantTableRepository = restaurantTableRepository;
    }

    /**
     * Получить стол по ID.
     */
    public RestaurantTable getTableById(Long id) {
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
    public void deleteTable(Long id) {
        restaurantTableRepository.deleteById(id);
    }
    public List<RestaurantTable> getAvailableTablesByZone(String zoneName) {
        // Предположим, мы ищем доступные столы (available = true)
        return restaurantTableRepository.findByZoneAndAvailable(zoneName, true);
    }

}