package com.bootcamp.bootcamp.domain.exception;

public enum DomainErrorCode {

    NAME_REQUIRED("El nombre es obligatorio"),
    NAME_TOO_LONG("El nombre excede la longitud máxima de 50 caracteres"),
    DESCRIPTION_REQUIRED("La descripción es obligatoria"),
    DESCRIPTION_TOO_LONG("La descripción excede la longitud máxima de 90 caracteres"),
    LAUNCH_DATE_REQUIRED("La fecha de lanzamiento es obligatoria"),
    DURATION_INVALID("La duración debe ser un entero positivo de días"),
    CAPABILITIES_TOO_FEW("Un bootcamp debe tener como mínimo 1 capacidad"),
    CAPABILITIES_TOO_MANY("Un bootcamp debe tener como máximo 4 capacidades"),
    CAPABILITIES_DUPLICATED("No se permiten capacidades repetidas");

    private final String message;

    DomainErrorCode(String message) {
        this.message = message;
    }

    public String getCode() {
        return name();
    }

    public String getMessage() {
        return message;
    }
}
