package com.fransua.appointment.guest.appointment.dao;

import com.fransua.appointment.guest.appointment.Appointment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  @Query(
      """
      SELECT COUNT(a) > 0
      FROM Appointment a
      WHERE a.shiftId = :shiftId
        AND a.status NOT IN ('CANCELLED_BY_GUEST', 'CANCELLED_BY_MASTER')
        AND a.startTime < :requestedEndTime
        AND a.endTime > :requestedStartTime
      """)
  boolean hasOverlappingAppointments(
      @Param("shiftId") Long shiftId,
      @Param("requestedStartTime") LocalTime requestedStartTime,
      @Param("requestedEndTime") LocalTime requestedEndTime);

  List<Appointment> findAllByAddressIdAndDateInAndStatusNotIn(
      Long addressId, Set<LocalDate> dates, Set<Appointment.Status> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
  @Query("SELECT a FROM Appointment a WHERE a.status = 'CREATED' ORDER BY a.createdAt" + " ASC")
  List<Appointment> findTopForVerification(Pageable pageable);
}
