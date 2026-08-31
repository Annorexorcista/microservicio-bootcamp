package com.bootcamp.bootcamp.domain.model;

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
