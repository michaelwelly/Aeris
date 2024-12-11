package com.aeris.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_tables")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone", nullable = false)
    private String zone; // Зона, например, "бар", "основной зал"

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber; // Номер столика

    @Column(name = "capacity", nullable = false)
    private Integer capacity; // Вместимость столика

    @Column(name = "description")
    private String description; // Дополнительное описание (опционально)

    @Column(name = "available", nullable = false)
    private Boolean available = true; // Доступность столика (по умолчанию - доступен)

    // Геттеры
    public Long getId() {
        return id;
    }

    public String getZone() {
        return zone;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getAvailable() {
        return available;
    }

    // Сеттеры
    public void setId(Long id) {
        this.id = id;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}