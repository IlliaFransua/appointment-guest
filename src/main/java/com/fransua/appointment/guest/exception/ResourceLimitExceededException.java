package com.fransua.appointment.guest.exception;

public class ResourceLimitExceededException extends RuntimeException {

  public ResourceLimitExceededException(String resourceName, int maxLimit) {
    super(
        "You have reached the limit: You cannot create more than %d %s"
            .formatted(maxLimit, resourceName.toLowerCase()));
  }

  public ResourceLimitExceededException(String message) {
    super(message);
  }
}
