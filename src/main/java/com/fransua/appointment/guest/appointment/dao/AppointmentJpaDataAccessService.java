package com.fransua.appointment.guest.appointment.dao;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.appointment.Appointment.Status;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AppointmentJpaDataAccessService implements AppointmentDao {

  private final AppointmentRepository repository;

  @Override
  public boolean hasOverlappingAppointments(
      Long shiftId, LocalTime requestedStartTime, LocalTime requestedEndTime) {
    return repository.hasOverlappingAppointments(shiftId, requestedStartTime, requestedEndTime);
  }

  @Override
  public Appointment save(Appointment entity) {
    return repository.save(entity);
  }

  @Override
  public List<Appointment> findAllByAddressIdAndDateInAndStatusNotIn(
      Long addressId, Set<LocalDate> dates, Set<Status> statuses) {
    return repository.findAllByAddressIdAndDateInAndStatusNotIn(addressId, dates, statuses);
  }

  @Override
  public List<Appointment> findTopForVerification(Pageable pageable) {
    return repository.findTopForVerification(pageable);
  }
}
