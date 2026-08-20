package com.riogandarilla.api.services;

import com.riogandarilla.api.dto.response.AnnualDashboardPoint;
import com.riogandarilla.api.dto.response.DashboardPaymentStatus;
import com.riogandarilla.api.dto.response.DashboardSummary;
import com.riogandarilla.api.entities.PaymentMovement;

import java.util.List;

public interface DashboardService {
    DashboardSummary summary(int month, int year);

    DashboardPaymentStatus paymentStatus(int month, int year);

    List<AnnualDashboardPoint> annualHistory(int year);

    List<PaymentMovement> recent(int month, int year);
}
