package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.dto.request.CreateCommitteeExpenseRequest;
import com.riogandarilla.api.entities.CommitteeExpense;
import com.riogandarilla.api.dto.response.MonthlyExpenseSummary;
import com.riogandarilla.api.repositories.CommitteeExpenseRepository;
import com.riogandarilla.api.services.CommitteeExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommitteeExpenseServiceImpl implements CommitteeExpenseService {
    private final CommitteeExpenseRepository repository;

    public CommitteeExpenseServiceImpl(CommitteeExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CommitteeExpense create(CreateCommitteeExpenseRequest request) {
        return repository.create(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitteeExpense> findByPeriod(int month, int year) {
        return repository.findByPeriod(month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyExpenseSummary> annualSummary(int year) {
        return repository.annualSummary(year);
    }
}
