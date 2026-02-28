package com.fransua.appointment.guest.master.dto.shift;

import java.time.LocalTime;

public record PauseResponse(LocalTime startTime, LocalTime endTime) {}
