package com.fransua.appointment.guest.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateAppointmentRequest(
    @NotNull Long addressId,
    @NotNull Long offeringId,
    @NotNull Long shiftId,
    @Size(min = 2, max = 50) String guestName,
    @Size(max = 500) String guestPreAppointmentNotes,
    @NotNull LocalTime startTime) {}
