package com.bootcamp.bootcamp.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de dominio puro que representa un bootcamp.
 *
 * <p>Clase inmutable sin anotaciones de framework: sus valores se fijan en el
 * constructor y solo se exponen mediante getters (no hay setters). El mapeo a
 * persistencia (R2DBC) y a transporte (DTOs) ocurre en los adaptadores, por lo
 * que este modelo permanece libre de acoplamiento a Spring, R2DBC o Jackson.
 *
 * <p>Un bootcamp agrupa capacidades; persiste únicamente los identificadores de
 * capacidad ({@code capabilityIds}), ya que el catálogo de capacidades es
 * propiedad del microservicio de Capacidad.
 */
public final class Bootcamp {

    private final Long id;
    private final String name;
    private final String description;
    private final LocalDate launchDate;
    private final int durationInDays;
    private final List<Long> capabilityIds;

    public Bootcamp(Long id,
                    String name,
                    String description,
                    LocalDate launchDate,
                    int durationInDays,
                    List<Long> capabilityIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.launchDate = launchDate;
        this.durationInDays = durationInDays;
        this.capabilityIds = capabilityIds;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getLaunchDate() {
        return launchDate;
    }

    public int getDurationInDays() {
        return durationInDays;
    }

    public List<Long> getCapabilityIds() {
        return capabilityIds;
    }
}
