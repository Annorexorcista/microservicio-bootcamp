package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.exception.InvalidPageQueryException;
import com.bootcamp.bootcamp.domain.exception.PageErrorCode;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Feature: listar-bootcamps, Property 4 — Validates: Requirements 4.3, 4.4, 4.5
 *
 * <p>Un query con page < 0, size < 1 o size > 100 emite
 * {@link InvalidPageQueryException} y no invoca findPage, countAll ni el gateway.
 */
class BootcampListProperty4Test {

    @Property(tries = 200)
    void negativePageRejected(@ForAll @IntRange(min = 1, max = 100_000) int magnitude,
                              @ForAll @IntRange(min = 1, max = 100) int size) {
        assertRejected(new BootcampPageQuery(-magnitude, size,
                BootcampSortBy.NAME, BootcampSortDirection.ASC), PageErrorCode.PAGE_NEGATIVE);
    }

    @Property(tries = 200)
    void tooSmallSizeRejected(@ForAll @IntRange(min = 0, max = 10_000) int page,
                              @ForAll @IntRange(min = 0, max = 100_000) int magnitude) {
        assertRejected(new BootcampPageQuery(page, -magnitude,
                BootcampSortBy.NAME, BootcampSortDirection.ASC), PageErrorCode.SIZE_TOO_SMALL);
    }

    @Property(tries = 200)
    void tooLargeSizeRejected(@ForAll @IntRange(min = 0, max = 10_000) int page,
                              @ForAll @IntRange(min = 101, max = 1_000_000) int size) {
        assertRejected(new BootcampPageQuery(page, size,
                BootcampSortBy.NAME, BootcampSortDirection.ASC), PageErrorCode.SIZE_TOO_LARGE);
    }

    private void assertRejected(BootcampPageQuery query, PageErrorCode expected) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);
        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);

        Throwable error = null;
        try {
            useCase.listBootcamps(query).block();
        } catch (Throwable t) {
            error = t;
        }
        if (!(error instanceof InvalidPageQueryException ipe) || ipe.getCode() != expected) {
            throw new AssertionError("esperado " + expected + ", obtenido=" + error);
        }
        verify(persistencePort, never()).findPage(any());
        verify(persistencePort, never()).countAll();
        verify(gatewayPort, never()).findCapabilitiesByIds(anyCollection());
    }
}
