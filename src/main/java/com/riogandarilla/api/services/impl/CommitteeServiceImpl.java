package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.entities.CommitteeAssignment;
import com.riogandarilla.api.exception.ValidationException;
import com.riogandarilla.api.repositories.CommitteeRepository;
import com.riogandarilla.api.services.CommitteeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class CommitteeServiceImpl implements CommitteeService {

    private final CommitteeRepository repository;

    public CommitteeServiceImpl(CommitteeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> housesForDate(LocalDate date) {
        return repository.findHouseNumbersForDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitteeAssignment> history() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void replaceFrom(LocalDate startDate, List<Integer> houseNumbers) {
        List<Integer> normalized = houseNumbers == null ? List.of() : houseNumbers.stream().distinct().sorted().toList();
        if (normalized.stream().anyMatch(house -> house == null || house < 1 || house > 50)) {
            throw new ValidationException("INVALID_COMMITTEE_HOUSE", "Las casas de comité deben estar entre 1 y 50");
        }
        repository.replaceFrom(startDate, normalized);
    }
}
