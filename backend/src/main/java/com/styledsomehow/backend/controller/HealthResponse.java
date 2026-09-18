package com.styledsomehow.backend.controller;

public record HealthResponse(String status, String service) {

	public static HealthResponse of(String status, String service) {
		return new HealthResponse(status, service);
	}

}