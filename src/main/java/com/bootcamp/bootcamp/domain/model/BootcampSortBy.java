package com.bootcamp.bootcamp.domain.model;

/**
 * Criterio de ordenamiento del listado de bootcamps.
 *
 * <p>Enum puro del dominio. La traducción desde los valores de la API
 * ({@code name}, {@code capabilityCount}) ocurre en la capa driving, y la
 * traducción hacia fragmentos SQL de lista blanca ocurre en el adaptador de
 * persistencia.
 */
public enum BootcampSortBy {
    NAME,
    CAPABILITY_COUNT
}
