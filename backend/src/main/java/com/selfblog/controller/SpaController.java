package com.selfblog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the Vue history-mode routes from the bundled frontend. */
@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}",
            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
