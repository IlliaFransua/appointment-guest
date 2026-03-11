package com.fransua.appointment.guest.exception;

public class ServiceUnavailableException extends RuntimeException {

  public ServiceUnavailableException(String message) {
    super(message);
  }
}
