package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.entities.Resident;
import com.riogandarilla.api.repositories.ResidentRepository;
import com.riogandarilla.api.services.ResidentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository repository;

    public ResidentServiceImpl(ResidentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resident> findAllActive() {
        return repository.findAllActive();
    }
}
