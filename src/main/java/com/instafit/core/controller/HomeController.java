
package com.instafit.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    @PreAuthorize("hasRole('OPERATION')")
    public String home(Model model) {
        model.addAttribute("activePage", "home");
        return "home";
    }
}