package com.fransua.appointment.guest.exception;

public class VerificationCodeExpiredException extends RuntimeException {

  public VerificationCodeExpiredException(Long appointmentId) {
    super("Verification code has expired. Please request a new one.");
  }
}
