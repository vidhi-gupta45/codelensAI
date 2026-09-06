package com.codelens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> getProtectedResource() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = Map.of(
                "message", "Access Granted! You have accessed a protected endpoint.",
                "userEmail", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        );

        return ResponseEntity.ok(response);
    }
}
