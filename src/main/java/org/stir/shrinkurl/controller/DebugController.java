package org.stir.shrinkurl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {
    @GetMapping("/test")
    public String test() {
        System.out.println(">>> /test hit");
        return "OK";
    }
}
