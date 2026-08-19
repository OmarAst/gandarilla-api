package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.dto.request.CreatePaymentMovementsBatchRequest;
import com.riogandarilla.api.dto.response.PaymentMethodResponse;
import com.riogandarilla.api.dto.response.PaymentMovementResponse;
import com.riogandarilla.api.dto.response.PaymentMovementsBatchResponse;
import com.riogandarilla.api.entities.PaymentMethod;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.entities.Resident;
import com.riogandarilla.api.exception.ResourceNotFoundException;
import com.riogandarilla.api.exception.ValidationException;
import com.riogandarilla.api.repositories.PaymentMovementRepository;
import com.riogandarilla.api.repositories.ResidentRepository;
import com.riogandarilla.api.services.PaymentMovementService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class PaymentMovementServiceImpl implements PaymentMovementService {

    private final ResidentRepository residentRepository;
    private final PaymentMovementRepository movementRepository;

    public PaymentMovementServiceImpl(
            ResidentRepository residentRepository,
            PaymentMovementRepository movementRepository
    ) {
        this.residentRepository = residentRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional
    public PaymentMovementResponse create(CreatePaymentMovementRequest request) {
        Resident resident = residentRepository.findActiveByHouseNumber(request.casa())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACTIVE_RESIDENT_NOT_FOUND",
                        "No se encontró un residente activo para la casa " + request.casa()
                ));

        return createForResident(resident, request);
    }

    @Override
    @Transactional
    public PaymentMovementsBatchResponse createBatch(CreatePaymentMovementsBatchRequest request) {
        if (request.casas().stream().distinct().count() != request.casas().size()) {
            throw new ValidationException(
                    "DUPLICATE_HOUSES",
                    "La lista de casas no puede contener números repetidos"
            );
        }

        PaymentMethod.fromId(request.formaPago());
        Map<Integer, Resident> residents = new LinkedHashMap<>();
        for (Integer house : request.casas()) {
            Resident resident = residentRepository.findActiveByHouseNumber(house)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ACTIVE_RESIDENT_NOT_FOUND",
                            "No se encontró un residente activo para la casa " + house
                    ));
            residents.put(house, resident);
        }

        List<PaymentMovementResponse> movements = residents.entrySet().stream()
                .map(entry -> createForResident(entry.getValue(), request.forHouse(entry.getKey())))
                .toList();
        log.info("Lote de movimientos registrado total={} period={}/{}",
                movements.size(), request.mes(), request.anio());
        return new PaymentMovementsBatchResponse(movements.size(), movements);
    }

    private PaymentMovementResponse createForResident(
            Resident resident,
            CreatePaymentMovementRequest request
    ) {
        PaymentMethod.fromId(request.formaPago());
        PaymentMovement movement = movementRepository.create(resident.id(), request);

        log.info(
                "Movimiento de pago registrado movementId={} folio={} house={} period={}/{}",
                movement.id(),
                movement.folio(),
                movement.numCasa(),
                movement.mes(),
                movement.anio()
        );
        return toResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMovementResponse findById(long id) {
        return toResponse(findMovement(id));
    }

    private PaymentMovement findMovement(long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_MOVEMENT_NOT_FOUND",
                        "No se encontró el movimiento de pago " + id
                ));
    }

    public static PaymentMovementResponse toResponse(PaymentMovement movement) {
        PaymentMethod method = PaymentMethod.fromId(movement.formaPago());
        return new PaymentMovementResponse(
                movement.id(),
                movement.folio(),
                movement.residenteId(),
                movement.residenteNombre(),
                movement.numCasa(),
                movement.monto(),
                movement.observaciones(),
                new PaymentMethodResponse(method.id(), method.descripcion()),
                movement.fechaPago(),
                movement.mes(),
                movement.anio(),
                movement.estatus(),
                movement.whatsappEnviado(),
                movement.fechaEnvioWhatsapp(),
                movement.whatsappMessageId(),
                movement.fechaCreacion()
        );
    }
}
