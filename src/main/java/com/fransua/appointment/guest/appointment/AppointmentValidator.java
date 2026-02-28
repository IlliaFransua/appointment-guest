package com.fransua.appointment.guest.appointment;

import com.fransua.appointment.guest.appointment.dao.AppointmentDao;
import com.fransua.appointment.guest.appointment.dto.CreateAppointmentRequest;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceLimitExceededException;
import com.fransua.appointment.guest.master.dto.booking.BookingContextResponse;
import com.fransua.appointment.guest.master.dto.shift.PauseResponse;
import com.fransua.appointment.guest.master.dto.shift.ShiftResponse;
import com.fransua.appointment.guest.util.FTimeUtil;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentValidator {

  private final AppointmentDao appointmentDao;

  @Transactional(readOnly = true)
  public void validate(BookingContextResponse context, CreateAppointmentRequest request) {
    boolean isAvailableAtAddress =
        context.offering().prices().stream()
            .anyMatch(price -> price.addressId().equals(request.addressId()));

    if (!isAvailableAtAddress) {
      throw new RequestValidationException("Offering not available at this location");
    }

    checkTimeSlotAvailability(context, request);
  }

  private void checkTimeSlotAvailability(
      BookingContextResponse context, CreateAppointmentRequest request) {
    LocalTime startTime = request.startTime();
    LocalTime endTime = startTime.plusMinutes(context.offering().durationMaxMinutes().longValue());

    ShiftResponse shift = context.shift();
    if (!FTimeUtil.containsInclusive(shift.startTime(), shift.endTime(), startTime, endTime)) {
      throw new ResourceLimitExceededException(
          String.format(
              "Requested time [%s - %s] is outside of master's shift hours [%s - %s]",
              startTime, endTime, shift.startTime(), shift.endTime()));
    }

    for (PauseResponse pause : shift.pauses()) {
      if (FTimeUtil.isOverlapping(startTime, endTime, pause.startTime(), pause.endTime())) {
        throw new ResourceLimitExceededException(
            String.format(
                "Requested time [%s - %s] overlaps with a shift pause [%s - %s]",
                startTime, endTime, pause.startTime(), pause.endTime()));
      }
    }

    if (appointmentDao.hasOverlappingAppointments(shift.id(), startTime, endTime)) {
      throw new ResourceLimitExceededException(
          String.format(
              "Requested slot [%s - %s] is already occupied by another appointment",
              startTime, endTime));
    }
  }
}
