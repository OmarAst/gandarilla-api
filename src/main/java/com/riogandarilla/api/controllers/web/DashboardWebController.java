package com.riogandarilla.api.controllers.web;

import com.riogandarilla.api.services.DashboardService;
import com.riogandarilla.api.services.CommitteeExpenseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardWebController {

    private static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    );

    private final DashboardService dashboardService;
    private final CommitteeExpenseService expenseService;
    private final Clock clock;

    public DashboardWebController(
            DashboardService dashboardService,
            CommitteeExpenseService expenseService,
            Clock clock
    ) {
        this.dashboardService = dashboardService;
        this.expenseService = expenseService;
        this.clock = clock;
    }

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/web/dashboard");
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin-login";
    }

    @GetMapping("/web/dashboard")
    public String dashboard(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
                Model model,
                Authentication authentication
    ) {
        LocalDate today = LocalDate.now(clock);
        int selectedMonth = mes == null || mes < 1 || mes > 12 ? today.getMonthValue() : mes;
        int selectedYear = anio == null || anio < 2020 || anio > 2100 ? today.getYear() : anio;
        model.addAttribute("summary", dashboardService.summary(selectedMonth, selectedYear));
        model.addAttribute("recent", dashboardService.recent(selectedMonth, selectedYear));
        model.addAttribute("expenses", expenseService.findByPeriod(selectedMonth, selectedYear));
        model.addAttribute("annualExpenses", expenseService.annualSummary(selectedYear));
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthName", MONTH_NAMES.get(selectedMonth - 1));
        model.addAttribute("monthNames", MONTH_NAMES);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("isAdmin", authentication != null
            && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        model.addAttribute("adminUsername", authentication == null ? null : authentication.getName());
        return "dashboard";
    }

    public String dashboard(Integer mes, Integer anio, Model model) {
        return dashboard(mes, anio, model, null);
    }
}
