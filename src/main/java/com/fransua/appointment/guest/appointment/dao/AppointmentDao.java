package com.fransua.appointment.guest.appointment.dao;

import com.fransua.appointment.guest.appointment.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;

public interface AppointmentDao {

  boolean hasOverlappingAppointments(
      Long shiftId, LocalTime requestedStartTime, LocalTime requestedEndTime);

  Appointment save(Appointment entity);

  List<Appointment> findAllByAddressIdAndDateInAndStatusNotIn(
      Long addressId, Set<LocalDate> dates, Set<Appointment.Status> statuses);

  List<Appointment> findTopForVerification(Pageable pageable);
}
