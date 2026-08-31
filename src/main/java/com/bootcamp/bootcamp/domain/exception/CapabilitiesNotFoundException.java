package com.bootcamp.bootcamp.domain.exception;

import java.util.List;

public class CapabilitiesNotFoundException extends RuntimeException {

    private final List<Long> missingIds;

    public CapabilitiesNotFoundException(List<Long> missingIds) {
        super("Las siguientes capacidades no existen: " + missingIds);
        this.missingIds = List.copyOf(missingIds);
    }

    public List<Long> getMissingIds() {
        return missingIds;
    }
}
