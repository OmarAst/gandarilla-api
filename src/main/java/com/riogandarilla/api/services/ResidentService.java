package com.riogandarilla.api.services;

import com.riogandarilla.api.entities.Resident;

import java.util.List;

public interface ResidentService {
    List<Resident> findAllActive();
}
