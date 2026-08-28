package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.mapper;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper puro (sin I/O ni tipos reactivos) que convierte entre el modelo de
 * dominio {@link Bootcamp} y la entidad de persistencia {@link BootcampEntity}.
 *
 * <p>Las conversiones son transformaciones en memoria; se invocan dentro del
 * pipeline reactivo del adaptador, por lo que este componente no conoce Project
 * Reactor ni R2DBC. La asociación N:M con las capacidades no se representa en
 * {@link BootcampEntity} (vive en la tabla puente), por lo que
 * {@link #toDomain(BootcampEntity, List)} recibe los identificadores de
 * capacidad por separado.
 */
@Component
public class BootcampEntityMapper {

    /**
     * Convierte un modelo de dominio en entidad de persistencia.
     *
     * <p>Cuando el {@code id} del dominio es {@code null}, el {@code id} de la
     * entidad también queda en {@code null}, de modo que Spring Data R2DBC trate
     * la fila como nueva ({@code isNew}) y ejecute un INSERT.
     *
     * @param bootcamp modelo de dominio a convertir; puede ser {@code null}
     * @return la entidad equivalente, o {@code null} si {@code bootcamp} es {@code null}
     */
    public BootcampEntity toEntity(Bootcamp bootcamp) {
        if (bootcamp == null) {
            return null;
        }
        return new BootcampEntity(
                bootcamp.getId(),
                bootcamp.getName(),
                bootcamp.getDescription(),
                bootcamp.getLaunchDate(),
                bootcamp.getDurationInDays());
    }

    /**
     * Convierte una entidad de persistencia y su lista de identificadores de
     * capacidad asociados en un modelo de dominio.
     *
     * @param entity        entidad a convertir; puede ser {@code null}
     * @param capabilityIds identificadores de capacidad asociados al bootcamp
     * @return el modelo de dominio equivalente, o {@code null} si {@code entity} es {@code null}
     */
    public Bootcamp toDomain(BootcampEntity entity, List<Long> capabilityIds) {
        if (entity == null) {
            return null;
        }
        return new Bootcamp(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLaunchDate(),
                entity.getDurationDays() == null ? 0 : entity.getDurationDays(),
                capabilityIds);
    }
}
