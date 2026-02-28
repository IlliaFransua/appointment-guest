package com.fransua.appointment.guest.master.dto.offering;

import java.time.LocalDate;
import java.util.List;

public record DayAvailabilityResponse(LocalDate date, List<TimeIntervalResponse> freeTimes) {}
