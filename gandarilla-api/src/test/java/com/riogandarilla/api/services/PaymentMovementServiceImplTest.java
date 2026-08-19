package com.riogandarilla.api.services;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.dto.request.CreatePaymentMovementsBatchRequest;
import com.riogandarilla.api.dto.response.PaymentMovementResponse;
import com.riogandarilla.api.dto.response.PaymentMovementsBatchResponse;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.entities.Resident;
import com.riogandarilla.api.repositories.PaymentMovementRepository;
import com.riogandarilla.api.repositories.ResidentRepository;
import com.riogandarilla.api.exception.ResourceNotFoundException;
import com.riogandarilla.api.exception.ValidationException;
import com.riogandarilla.api.services.impl.PaymentMovementServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class PaymentMovementServiceImplTest {

    @Test
    void shouldCreatePaymentForActiveResident() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(
                residentRepository,
                movementRepository
        );

        CreatePaymentMovementRequest request = new CreatePaymentMovementRequest(
                42,
                new BigDecimal("900.00"),
                "Pago de julio",
                1,
                LocalDate.of(2026, 7, 7),
                7,
                2026
        );

        Resident resident = new Resident(
                42L,
                "Daniel Salazar Sánchez",
                "526670000042",
                42,
                true,
                OffsetDateTime.parse("2026-01-01T10:00:00-07:00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-07:00")
        );
        PaymentMovement movement = new PaymentMovement(
                1L,
                42L,
                resident.nombre(),
                resident.telefono(),
                42,
                true,
                request.monto(),
                request.observaciones(),
                request.formaPago(),
                request.fechaPago(),
                request.mes(),
                request.anio(),
                "GAN-2026-000001",
                "REGISTRADO",
                false,
                null,
                null,
                OffsetDateTime.parse("2026-07-07T10:00:00-07:00"),
                OffsetDateTime.parse("2026-07-07T10:00:00-07:00")
        );

        when(residentRepository.findActiveByHouseNumber(42)).thenReturn(Optional.of(resident));
        when(movementRepository.create(42L, request)).thenReturn(movement);

        PaymentMovementResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.folio()).isEqualTo("GAN-2026-000001");
        assertThat(response.casa()).isEqualTo(42);
        assertThat(response.formaPago().descripcion()).isEqualTo("Transferencia");
    }

    @Test
    void shouldReturnNotFoundWhenHouseHasNoActiveResident() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(residentRepository, movementRepository);
        CreatePaymentMovementRequest request = new CreatePaymentMovementRequest(
                1, new BigDecimal("800.00"), null, 1,
                LocalDate.of(2026, 8, 4), 8, 2026
        );
        when(residentRepository.findActiveByHouseNumber(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(error -> ((ResourceNotFoundException) error).status().value())
                .isEqualTo(404);
    }

    @Test
    void shouldFindMovementById() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(residentRepository, movementRepository);
        PaymentMovement movement = new PaymentMovement(
                1L, 4L, "Omar Astorga", "526670000001", 1, true,
                new BigDecimal("800.00"), null, 1, LocalDate.of(2026, 8, 4), 8, 2026,
                "GAN-2026-000001", "REGISTRADO", false, null, null,
                OffsetDateTime.parse("2026-08-04T12:00:00-07:00"),
                OffsetDateTime.parse("2026-08-04T12:00:00-07:00")
        );
        when(movementRepository.findById(1L)).thenReturn(Optional.of(movement));

        PaymentMovementResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.casa()).isEqualTo(1);
    }

    @Test
    void shouldCreateOneMovementForEveryHouseInBatch() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(residentRepository, movementRepository);
        CreatePaymentMovementsBatchRequest request = batchRequest(List.of(1, 2));
        Resident first = resident(1L, 1, "Lorena Salazar Vega");
        Resident second = resident(2L, 2, "Citlali Madueño");
        when(residentRepository.findActiveByHouseNumber(1)).thenReturn(Optional.of(first));
        when(residentRepository.findActiveByHouseNumber(2)).thenReturn(Optional.of(second));
        when(movementRepository.create(1L, request.forHouse(1)))
                .thenReturn(movement(1L, first, "GAN-2026-000001"));
        when(movementRepository.create(2L, request.forHouse(2)))
                .thenReturn(movement(2L, second, "GAN-2026-000002"));

        PaymentMovementsBatchResponse response = service.createBatch(request);

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.movimientos()).extracting(PaymentMovementResponse::casa)
                .containsExactly(1, 2);
    }

    @Test
    void shouldRejectRepeatedHousesWithoutCreatingMovements() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(residentRepository, movementRepository);

        assertThatThrownBy(() -> service.createBatch(batchRequest(List.of(1, 1))))
                .isInstanceOf(ValidationException.class);
        verify(movementRepository, never()).create(anyLong(), any());
    }

    @Test
    void shouldResolveAllResidentsBeforeWritingBatch() {
        ResidentRepository residentRepository = mock(ResidentRepository.class);
        PaymentMovementRepository movementRepository = mock(PaymentMovementRepository.class);
        PaymentMovementService service = new PaymentMovementServiceImpl(residentRepository, movementRepository);
        when(residentRepository.findActiveByHouseNumber(1))
                .thenReturn(Optional.of(resident(1L, 1, "Lorena Salazar Vega")));
        when(residentRepository.findActiveByHouseNumber(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBatch(batchRequest(List.of(1, 2))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("casa 2");
        verify(movementRepository, never()).create(anyLong(), any());
    }

    private CreatePaymentMovementsBatchRequest batchRequest(List<Integer> houses) {
        return new CreatePaymentMovementsBatchRequest(
                houses, new BigDecimal("800.00"), "Pago de agosto", 1,
                LocalDate.of(2026, 8, 4), 8, 2026
        );
    }

    private Resident resident(long id, int house, String name) {
        return new Resident(
                id, name, "5266700000" + String.format("%02d", house), house, true,
                OffsetDateTime.parse("2026-01-01T10:00:00-07:00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-07:00")
        );
    }

    private PaymentMovement movement(long id, Resident resident, String folio) {
        return new PaymentMovement(
                id, resident.id(), resident.nombre(), resident.telefono(), resident.numCasa(), true,
                new BigDecimal("800.00"), "Pago de agosto", 1,
                LocalDate.of(2026, 8, 4), 8, 2026, folio, "REGISTRADO",
                false, null, null,
                OffsetDateTime.parse("2026-08-04T10:00:00-07:00"),
                OffsetDateTime.parse("2026-08-04T10:00:00-07:00")
        );
    }
}
