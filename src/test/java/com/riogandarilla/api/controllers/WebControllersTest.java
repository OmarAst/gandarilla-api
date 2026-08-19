package com.riogandarilla.api.controllers;

import com.riogandarilla.api.controllers.web.DashboardWebController;
import com.riogandarilla.api.controllers.web.PaymentMovementWebController;
import com.riogandarilla.api.dto.response.DashboardSummary;
import com.riogandarilla.api.dto.response.PaymentMovementResponse;
import com.riogandarilla.api.entities.Resident;
import com.riogandarilla.api.services.DashboardService;
import com.riogandarilla.api.services.CommitteeExpenseService;
import com.riogandarilla.api.services.PaymentMovementService;
import com.riogandarilla.api.services.ReceiptService;
import com.riogandarilla.api.services.ResidentService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebControllersTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Mazatlan")
    );

    @Test
    void shouldPopulateDashboardForSelectedPeriod() {
        DashboardService service = mock(DashboardService.class);
        DashboardSummary summary = new DashboardSummary(
            8, 2026, new BigDecimal("6400.00"), new BigDecimal("0.00"),
            new BigDecimal("0.00"), new BigDecimal("6400.00"), new BigDecimal("33600.00"),
            8, 42, 8, 0
        );
        when(service.summary(8, 2026)).thenReturn(summary);
        when(service.recent(8, 2026)).thenReturn(List.of());
        CommitteeExpenseService expenses = mock(CommitteeExpenseService.class);
        when(expenses.findByPeriod(8, 2026)).thenReturn(List.of());
        when(expenses.annualSummary(2026)).thenReturn(List.of());
        DashboardWebController controller = new DashboardWebController(service, expenses, clock);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.dashboard(8, 2026, model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.get("summary")).isEqualTo(summary);
        assertThat(model.get("selectedMonth")).isEqualTo(8);
    }

    @Test
    void shouldLoadActiveResidentsIntoMovementsPage() {
        PaymentMovementService movements = mock(PaymentMovementService.class);
        ReceiptService receipts = mock(ReceiptService.class);
        ResidentService residents = mock(ResidentService.class);
        when(residents.findAllActive()).thenReturn(List.of(resident()));
        PaymentMovementWebController controller = controller(movements, receipts, residents);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.page(model);

        assertThat(view).isEqualTo("payment-movements");
        assertThat((List<?>) model.get("residents")).hasSize(1);
        assertThat(model.get("currentMonth")).isEqualTo(8);
    }

    @Test
    void shouldSubmitIndividualMovementThroughExistingService() {
        PaymentMovementService movements = mock(PaymentMovementService.class);
        ReceiptService receipts = mock(ReceiptService.class);
        ResidentService residents = mock(ResidentService.class);
        PaymentMovementResponse response = mock(PaymentMovementResponse.class);
        when(response.folio()).thenReturn("GAN-2026-000001");
        when(movements.create(any())).thenReturn(response);
        PaymentMovementWebController controller = controller(movements, receipts, residents);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.createSingle(
                1, new BigDecimal("800.00"), "Pago de agosto", 1,
                LocalDate.of(2026, 8, 4), 8, 2026, redirect
        );

        assertThat(view).isEqualTo("redirect:/web/payment-movements");
        assertThat(redirect.getFlashAttributes()).containsKey("success");
        verify(movements).create(any());
    }

    private PaymentMovementWebController controller(
            PaymentMovementService movements,
            ReceiptService receipts,
            ResidentService residents
    ) {
        return new PaymentMovementWebController(
                movements, receipts, residents,
                Validation.buildDefaultValidatorFactory().getValidator(), clock
        );
    }

    private Resident resident() {
        return new Resident(
                1L, "Lorena Salazar Vega", "526670000001", 1, true,
                null, null
        );
    }
}
