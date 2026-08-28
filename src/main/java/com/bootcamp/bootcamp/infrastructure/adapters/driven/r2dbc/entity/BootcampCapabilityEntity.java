package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entidad de persistencia R2DBC mapeada a la tabla puente {@code bootcamp_capability}.
 *
 * <p>Modela una fila de la relación N:M entre un bootcamp y una capacidad. La
 * tabla tiene una clave primaria compuesta {@code (bootcamp_id, capability_id)};
 * sin embargo, Spring Data R2DBC no soporta entidades con clave compuesta ni un
 * campo {@code @Id} multi-columna, por lo que esta entidad se modela como un
 * mapeo simple de las filas del join (sin {@code @Id}).
 *
 * <p>El adaptador de persistencia inserta estas filas explícitamente (una por
 * cada {@code capabilityId} asociado al bootcamp) mediante
 * {@code R2dbcEntityTemplate#insert}, ya que sin {@code @Id} no puede inferir si
 * la fila es nueva para un {@code save} basado en CRUD.
 */
@Table("bootcamp_capability")
public class BootcampCapabilityEntity {

    @Column("bootcamp_id")
    private Long bootcampId;

    @Column("capability_id")
    private Long capabilityId;

    public BootcampCapabilityEntity() {
    }

    public BootcampCapabilityEntity(Long bootcampId, Long capabilityId) {
        this.bootcampId = bootcampId;
        this.capabilityId = capabilityId;
    }

    public Long getBootcampId() {
        return bootcampId;
    }

    public void setBootcampId(Long bootcampId) {
        this.bootcampId = bootcampId;
    }

    public Long getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(Long capabilityId) {
        this.capabilityId = capabilityId;
    }
}
