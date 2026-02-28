package com.fransua.appointment.guest.master.dto.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ShiftResponse(
    Long id,
    Long addressId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    List<PauseResponse> pauses) {}
