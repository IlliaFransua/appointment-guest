package com.fransua.appointment.guest.exception.infra;

import lombok.Getter;

@Getter
public class ServiceIntegrationException extends RuntimeException {

  private final int status;

  private final ApiErrorResponse errorResponse;

  public ServiceIntegrationException(int status, ApiErrorResponse errorResponse) {
    super("Master service error: %d - %s".formatted(status, errorResponse.title()));
    this.status = status;
    this.errorResponse = errorResponse;
  }
}
