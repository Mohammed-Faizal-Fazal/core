package com.instafit.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class JobMonitoringController {

    @GetMapping("/job-monitoring")
    @PreAuthorize("hasRole('OPERATION')")
    public String jobMonitoring(Model model) {
        model.addAttribute("activePage", "job-monitoring");
        return "job-monitoring";
    }
}