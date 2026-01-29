package com.instafit.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SubmittedOrdersController {

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @GetMapping("/submitted-orders")
    @PreAuthorize("hasRole('OPERATION')")
    public String submittedOrders(Model model) {
        model.addAttribute("activePage", "submitted-orders");
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "submitted-orders";
    }
}