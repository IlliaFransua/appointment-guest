package com.fransua.appointment.guest.exception;

import java.time.LocalTime;
import java.time.YearMonth;

public class InvalidAppointmentSlotException extends RuntimeException {

  public InvalidAppointmentSlotException(
      LocalTime startTime, LocalTime endTime, YearMonth yearMonth) {
    super(
        String.format(
            "Time range %s - %s on %s is not aligned with any available booking slots",
            startTime, endTime, yearMonth));
  }
}
