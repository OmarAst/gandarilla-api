package com.riogandarilla.api.services;

import com.riogandarilla.api.dto.request.CreateCommitteeExpenseRequest;
import com.riogandarilla.api.entities.CommitteeExpense;
import com.riogandarilla.api.dto.response.MonthlyExpenseSummary;

import java.util.List;

public interface CommitteeExpenseService {
    CommitteeExpense create(CreateCommitteeExpenseRequest request);
    List<CommitteeExpense> findByPeriod(int month, int year);
    List<MonthlyExpenseSummary> annualSummary(int year);
}
