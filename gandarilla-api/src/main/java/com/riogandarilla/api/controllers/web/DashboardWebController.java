package com.riogandarilla.api.controllers.web;

import com.riogandarilla.api.services.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Clock;
import java.time.LocalDate;

@Controller
public class DashboardWebController {

    private final DashboardService dashboardService;
    private final Clock clock;

    public DashboardWebController(DashboardService dashboardService, Clock clock) {
        this.dashboardService = dashboardService;
        this.clock = clock;
    }

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/web/dashboard");
    }

    @GetMapping("/web/dashboard")
    public String dashboard(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            Model model
    ) {
        LocalDate today = LocalDate.now(clock);
        int selectedMonth = mes == null || mes < 1 || mes > 12 ? today.getMonthValue() : mes;
        int selectedYear = anio == null || anio < 2020 || anio > 2100 ? today.getYear() : anio;
        model.addAttribute("summary", dashboardService.summary(selectedMonth, selectedYear));
        model.addAttribute("recent", dashboardService.recent(selectedMonth, selectedYear));
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        return "dashboard";
    }
}
