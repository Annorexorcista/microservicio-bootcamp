package com.bootcamp.bootcamp.domain.exception;

/**
 * Códigos de error de negocio del dominio de bootcamps.
 *
 * <p>Cada código asocia una regla de validación sintáctica (obligatoriedad,
 * longitudes, fecha, duración, cantidad y no repetición de capacidades) con su
 * mensaje de negocio, manteniendo los textos centralizados y libres de
 * acoplamiento HTTP.
 */
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

    /**
     * @return el código de negocio (nombre de la constante del enum).
     */
    public String getCode() {
        return name();
    }

    /**
     * @return el mensaje de negocio asociado al código de error.
     */
    public String getMessage() {
        return message;
    }
}
