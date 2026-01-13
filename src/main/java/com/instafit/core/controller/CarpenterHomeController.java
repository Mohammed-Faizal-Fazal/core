package com.instafit.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/carpenter")
@PreAuthorize("hasRole('CARPENTER')")
public class CarpenterHomeController {

    @GetMapping("/home")
    public String carpenterHome(Model model) {
        model.addAttribute("activePage", "carpenter-home");
        return "carpenter/home";
    }

    @GetMapping("/my-jobs")
    public String myJobs(Model model) {
        model.addAttribute("activePage", "my-jobs");
        return "carpenter/my-jobs";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("activePage", "profile");
        return "carpenter/profile";
    }
}