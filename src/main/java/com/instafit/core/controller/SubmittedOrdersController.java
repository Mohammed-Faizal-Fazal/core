package com.instafit.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SubmittedOrdersController {

    @GetMapping("/submitted-orders")
    @PreAuthorize("hasRole('OPERATION')")
    public String submittedOrders(Model model) {
        model.addAttribute("activePage", "submitted-orders");
        return "submitted-orders";
    }
}