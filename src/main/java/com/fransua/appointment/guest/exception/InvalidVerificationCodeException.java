package com.fransua.appointment.guest.exception;

public class InvalidVerificationCodeException extends RuntimeException {

  public InvalidVerificationCodeException(String code) {
    super("Invalid verification code [%s]".formatted(code));
  }
}
