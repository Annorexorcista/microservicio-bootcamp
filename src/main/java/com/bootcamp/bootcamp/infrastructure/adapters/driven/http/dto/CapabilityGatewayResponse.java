package com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto;

/**
 * DTO de respuesta del Capability_Service para la consulta por identificadores
 * {@code GET /api/v1/capabilities?ids=1,2,3}.
 *
 * <p>El endpoint devuelve un arreglo JSON con las capacidades existentes en el
 * formato {@code [{id, name, description}]}. Para validar existencia solo se
 * necesita el {@code id}; {@code name} y {@code description} se incluyen para
 * reflejar fielmente el contrato del servicio consumido.
 *
 * @param id          identificador de la capacidad existente.
 * @param name        nombre de la capacidad.
 * @param description descripción de la capacidad.
 */
public record CapabilityGatewayResponse(Long id, String name, String description) {
}
