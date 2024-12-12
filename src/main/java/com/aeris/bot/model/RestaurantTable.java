package com.aeris.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "tables")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "zone", nullable = false)
    private String zone; // Зона, например, "бар", "основной зал"

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber; // Номер столика

    @Column(name = "capacity", nullable = false)
    private Integer capacity; // Вместимость столика

    @Column(name = "description")
    private String description; // Дополнительное описание (опционально)

    @Column(name = "available", nullable = false)
    private Boolean available = true; // Доступность столика
}