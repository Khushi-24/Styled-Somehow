package com.styledsomehow.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
 @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource database;
 @GetMapping("/health/ready") public org.springframework.http.ResponseEntity<java.util.Map<String,String>> ready(){try(var connection=database.getConnection()){if(!connection.isValid(2))throw new java.sql.SQLException();return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status","UP"));}catch(java.sql.SQLException unavailable){return org.springframework.http.ResponseEntity.status(503).body(java.util.Map.of("status","UNAVAILABLE"));}}

	@GetMapping("/health")
	public HealthResponse health() {
		return HealthResponse.of("UP", "styled-somehow-backend");
	}

}
