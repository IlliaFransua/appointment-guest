package com.fransua.appointment.guest.appointment.dto;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.master.dto.booking.GuestFieldsResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record AppointmentResponse(
    Long id,
    Long masterId,

    // Guest

    String guestName,
    String guestPreAppointmentNotes,

    // Snapshot of shift

    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,

    // Snapshot of offering

    String offeringName,
    String offeringDescription,
    BigDecimal offeringPrice,

    // Snapshot of address

    String addressCountryCode, // ISO 3166-1 alpha-2
    String addressCurrencyCode, // ISO 4217
    String addressFull,
    String addressDetails,
    String addressTimezone, // IANA timezone
    Appointment.Status status,

    // Verification Strategies
    GuestFieldsResponse guestFieldsResponse) {}
