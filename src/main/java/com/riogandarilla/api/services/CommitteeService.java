package com.riogandarilla.api.services;

import com.riogandarilla.api.entities.CommitteeAssignment;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface CommitteeService {
    Set<Integer> housesForDate(LocalDate date);

    List<CommitteeAssignment> history();

    void replaceFrom(LocalDate startDate, List<Integer> houseNumbers);
}
