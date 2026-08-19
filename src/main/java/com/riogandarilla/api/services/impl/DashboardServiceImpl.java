package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.dto.response.DashboardSummary;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.repositories.DashboardRepository;
import com.riogandarilla.api.services.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository repository;

    public DashboardServiceImpl(DashboardRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary summary(int month, int year) {
        return repository.summary(month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMovement> recent(int month, int year) {
        return repository.recent(month, year);
    }
}
