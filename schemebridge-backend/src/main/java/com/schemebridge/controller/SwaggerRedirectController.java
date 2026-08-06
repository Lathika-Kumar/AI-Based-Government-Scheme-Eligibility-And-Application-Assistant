package com.schemebridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SwaggerRedirectController {

    @GetMapping("/")
    public RedirectView redirectToSwagger() {
        return new RedirectView("/swagger-ui/index.html", true, false);
    }

    @GetMapping({"/swagger-ui", "/swagger-ui.html"})
    public RedirectView redirectSwaggerUi() {
        return new RedirectView("/swagger-ui/index.html", true, false);
    }
}
