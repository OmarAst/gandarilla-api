package com.riogandarilla.api.controllers.web;

import com.riogandarilla.api.services.CommitteeService;
import com.riogandarilla.api.services.ResidentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/web/committee")
public class CommitteeWebController {

    private static final LocalDate DEFAULT_CURRENT_COMMITTEE_START = LocalDate.of(2026, 6, 1);

    private final CommitteeService committeeService;
    private final ResidentService residentService;

    public CommitteeWebController(CommitteeService committeeService, ResidentService residentService) {
        this.committeeService = committeeService;
        this.residentService = residentService;
    }

    @GetMapping
    public String page(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            Model model
    ) {
        LocalDate startDate = inicio == null ? DEFAULT_CURRENT_COMMITTEE_START : inicio.withDayOfMonth(1);
        model.addAttribute("residents", residentService.findAllActive());
        model.addAttribute("committeeHouses", committeeService.housesForDate(startDate));
        model.addAttribute("history", committeeService.history());
        model.addAttribute("startDate", startDate);
        return "committee-config";
    }

    @PostMapping
    public String save(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) List<Integer> casas,
            RedirectAttributes redirect
    ) {
        LocalDate startDate = inicio.withDayOfMonth(1);
        committeeService.replaceFrom(startDate, casas == null ? List.of() : casas);
        redirect.addFlashAttribute("success", "Configuración de comité actualizada desde " + startDate);
        return "redirect:/web/committee?inicio=" + startDate;
    }
}
