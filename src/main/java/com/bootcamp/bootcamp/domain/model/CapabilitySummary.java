package com.bootcamp.bootcamp.domain.model;

import java.util.List;

public final class CapabilitySummary {

    private final Long id;
    private final String name;
    private final List<TechnologySummary> technologies;

    public CapabilitySummary(Long id, String name, List<TechnologySummary> technologies) {
        this.id = id;
        this.name = name;
        this.technologies = List.copyOf(technologies);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TechnologySummary> getTechnologies() {
        return technologies;
    }
}
