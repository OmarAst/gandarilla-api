package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.dto.response.AnnualDashboardPoint;
import com.riogandarilla.api.dto.response.DashboardPaymentStatus;
import com.riogandarilla.api.dto.response.DashboardSummary;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.repositories.DashboardRepository;
import com.riogandarilla.api.services.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");

    private final DashboardRepository repository;
    private final AppProperties properties;
    private final Clock clock;

    public DashboardServiceImpl(DashboardRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary summary(int month, int year) {
        return repository.summary(month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardPaymentStatus paymentStatus(int month, int year) {
        LocalDate today = LocalDate.now(clock);
        DashboardRepository.PaymentStatusCounts counts = repository.paymentStatusCounts(
                month, year, properties.lateFromDay()
        );

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate currentMonth = today.withDayOfMonth(1);
        boolean futurePeriod = periodStart.isAfter(currentMonth);
        boolean overdue = !futurePeriod && isOverduePeriod(month, year, today);
        int overdueCount = overdue ? counts.sinPago() : 0;
        int pendingCount = futurePeriod || overdue ? 0 : counts.sinPago();
        int fee = applicableFee(month, year, today);
        BigDecimal pendingAmount = futurePeriod
                ? BigDecimal.ZERO
                : BigDecimal.valueOf((long) counts.sinPago() * fee);

        return new DashboardPaymentStatus(
                counts.pagadasATiempo(),
                counts.pagadasConAtraso(),
                overdueCount,
                pendingCount,
                counts.comiteExento(),
                overdueCount,
                pendingAmount,
                BigDecimal.valueOf(fee)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnualDashboardPoint> annualHistory(int year) {
        List<HistoryDraft> drafts = new ArrayList<>(12);
        BigDecimal max = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            DashboardSummary summary = repository.summary(month, year);
            DashboardPaymentStatus status = paymentStatus(month, year);
            max = max.max(summary.totalRecaudado());
            drafts.add(new HistoryDraft(month, summary.totalRecaudado(), status));
        }

        BigDecimal chartMax = max.signum() == 0 ? BigDecimal.ONE : max;
        return drafts.stream().map(draft -> new AnnualDashboardPoint(
                draft.month(),
                capitalize(Month.of(draft.month()).getDisplayName(TextStyle.FULL, ES_MX)),
                draft.totalRecaudado(),
                draft.status().pagadasATiempo(),
                draft.status().pagadasConAtraso(),
                draft.status().vencidas(),
                draft.status().pendientes(),
                draft.status().comiteExento(),
                draft.totalRecaudado()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(chartMax, 0, RoundingMode.HALF_UP)
                        .intValue()
        )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMovement> recent(int month, int year) {
        return repository.recent(month, year);
    }

    private boolean isOverduePeriod(int month, int year, LocalDate today) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate currentMonth = today.withDayOfMonth(1);
        if (periodStart.isBefore(currentMonth)) {
            return true;
        }
        return periodStart.equals(currentMonth) && today.getDayOfMonth() >= properties.blockFromDay();
    }

    private int applicableFee(int month, int year, LocalDate today) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate currentMonth = today.withDayOfMonth(1);
        if (periodStart.isAfter(currentMonth)) {
            return properties.regularMaintenanceFee();
        }
        if (periodStart.isBefore(currentMonth) || today.getDayOfMonth() >= properties.lateFromDay()) {
            return properties.lateMaintenanceFee();
        }
        return properties.regularMaintenanceFee();
    }

    private String capitalize(String value) {
        return value == null || value.isBlank()
                ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private record HistoryDraft(int month, BigDecimal totalRecaudado, DashboardPaymentStatus status) {
    }
}
