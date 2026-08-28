package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/**
 * Entidad de persistencia R2DBC mapeada a la tabla {@code bootcamp}.
 *
 * <p>Representa una fila de la tabla y se usa únicamente en la capa driven. El
 * mapeo entre esta entidad y el modelo de dominio {@code Bootcamp} lo realiza
 * {@code BootcampEntityMapper}, de modo que el dominio permanece libre de
 * anotaciones de framework.
 *
 * <p>Cuando {@code id} es {@code null}, Spring Data R2DBC considera que la fila
 * es nueva y ejecuta un {@code INSERT}; MySQL asigna el valor autoincremental.
 * Se exponen constructor sin argumentos, constructor con todos los argumentos y
 * getters/setters porque R2DBC los necesita para hidratar la entidad.
 */
@Table("bootcamp")
public class BootcampEntity {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("launch_date")
    private LocalDate launchDate;

    @Column("duration_days")
    private Integer durationDays;

    public BootcampEntity() {
    }

    public BootcampEntity(Long id, String name, String description,
                          LocalDate launchDate, Integer durationDays) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.launchDate = launchDate;
        this.durationDays = durationDays;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(LocalDate launchDate) {
        this.launchDate = launchDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }
}
