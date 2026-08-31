package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception;

public enum RequestErrorCode {

    ID_REQUIRED("El identificador del bootcamp es obligatorio"),
    ID_NOT_NUMERIC("El identificador '%s' debe ser numérico"),
    ID_NOT_POSITIVE("El identificador '%s' debe ser mayor que cero"),
    ID_OUT_OF_RANGE("El identificador '%s' está fuera del rango permitido");

    private final String messageTemplate;

    RequestErrorCode(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getCode() {
        return name();
    }

    public String formatMessage(Object... arguments) {
        return messageTemplate.formatted(arguments);
    }
}
