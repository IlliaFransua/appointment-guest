package com.fransua.appointment.guest.appointment.service;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.appointment.dao.AppointmentDao;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentCleanupService {

  private final AppointmentDao appointmentDao;

  @Transactional
  public int cleanNextBatch() {
    Instant threshold = Instant.now().minus(Duration.ofMinutes(5));
    List<Appointment> batch =
        appointmentDao.findTopForVerification(threshold, PageRequest.of(0, 20));

    if (batch.isEmpty()) {
      return 0;
    }

    for (Appointment appt : batch) {
      appointmentDao.delete(appt);
    }
    return batch.size();
  }
}
