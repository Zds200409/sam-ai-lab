package com.sam.membermgmt.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/home", "/member/**", "/product/**",
            "/order/**", "/points/**", "/statistics", "/system/**"})
    public String index() {
        return "index";
    }
}
