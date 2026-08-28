package com.bootcamp.bootcamp.domain.model;

/**
 * Parámetros de consulta ya tipados para el listado paginado y ordenado de
 * bootcamps.
 *
 * <p>Modelo de dominio puro e inmutable. La capa driving lo construye a partir de
 * los query params (aplicando defaults y traduciendo {@code sortBy}/
 * {@code sortDirection} a enums); el caso de uso valida el rango de
 * {@code page}/{@code size}.
 */
public final class BootcampPageQuery {

    private final int page;
    private final int size;
    private final BootcampSortBy sortBy;
    private final BootcampSortDirection direction;

    public BootcampPageQuery(int page, int size,
                             BootcampSortBy sortBy, BootcampSortDirection direction) {
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.direction = direction;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public BootcampSortBy getSortBy() {
        return sortBy;
    }

    public BootcampSortDirection getDirection() {
        return direction;
    }
}
