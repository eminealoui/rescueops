package tn.esprit.rescueops.dispatch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint de vérification de la chaîne gateway -> service (squelette séance 4). */
@RestController
@RequestMapping("/api/dispatch")
public class PingController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "dispatch-service", "status", "UP");
    }
}
