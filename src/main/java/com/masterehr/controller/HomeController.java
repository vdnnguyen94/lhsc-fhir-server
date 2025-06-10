package com.masterehr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a simple controller to handle non-FHIR web requests,
 * such as providing a basic status message at the root URL.
 */
@RestController
public class HomeController {

    /**
     * This method handles GET requests to the root ("/") of the server.
     * @return A simple string message confirming the server is running.
     */
    @GetMapping("/")
    public String getHome() {
        return "LHSC-FHIR-SERVER RUNNING";
    }
}
