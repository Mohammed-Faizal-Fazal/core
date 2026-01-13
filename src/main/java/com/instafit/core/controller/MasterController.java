package com.instafit.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Master Data Pages Controller
 */
@Controller
@RequestMapping("/master")
@PreAuthorize("hasRole('OPERATION')")
public class MasterController {

    @GetMapping("/cities")
    public String citiesPage(Model model) {
        model.addAttribute("activePage", "master");
        model.addAttribute("activeSubPage", "cities");
        return "master/cities";
    }
    @GetMapping("/fit-types")
    public String fitTypes(Model model) {
        model.addAttribute("activePage", "master");
        model.addAttribute("activeSubPage", "fit-types");
        return "master/fit-types";
    }

    @GetMapping("/items")
    public String itemsPage(Model model) {
        model.addAttribute("activePage", "master");
        model.addAttribute("activeSubPage", "items");
        return "master/items";
    }

    @GetMapping("/branches")
    public String branchesPage(Model model) {
        model.addAttribute("activePage", "master");
        model.addAttribute("activeSubPage", "branches");
        return "master/branches";
    }

    @GetMapping("/pincodes")
    public String pincodesPage(Model model) {
        model.addAttribute("activePage", "master");
        model.addAttribute("activeSubPage", "pincodes");
        return "master/pincodes";
    }
}