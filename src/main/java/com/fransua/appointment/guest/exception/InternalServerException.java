package com.fransua.appointment.guest.exception;

public class InternalServerException extends RuntimeException {

  public InternalServerException() {
    super("Master service is unavailable or returned unreadable error");
  }
}
