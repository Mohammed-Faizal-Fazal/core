package com.instafit.core.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Custom Error Controller
 * Handles application errors and displays custom error page
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("status", statusCode);

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Page Not Found");
                model.addAttribute("message", "The page you are looking for does not exist.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Internal Server Error");
                model.addAttribute("message", "An unexpected error occurred. Please try again later.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("error", "Access Denied");
                model.addAttribute("message", "You don't have permission to access this resource.");
            } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                model.addAttribute("error", "Unauthorized");
                model.addAttribute("message", "Please login to access this resource.");
            } else {
                model.addAttribute("error", "Error " + statusCode);
                model.addAttribute("message", "An error occurred while processing your request.");
            }
        }

        model.addAttribute("timestamp", new java.util.Date());
        return "error";
    }

    public String getErrorPath() {
        return "/error";
    }
}